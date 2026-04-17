package com.kodewerk.gcsee.cli.aggregation.heapstability;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Heap-stability gauge based on a Mann-Kendall trend test over the
 * post-collection heap occupancy series. A positive, statistically
 * significant trend means live-set is monotonically rising over time —
 * i.e., the application is likely leaking.
 *
 * Reports:
 *   - Mann-Kendall S statistic and z-score
 *   - Two-sided p-value
 *   - Theil-Sen slope (median of pairwise slopes) in MB/h
 *   - A categorical verdict
 *
 * Mann-Kendall is non-parametric (rank-based) and tolerant of outliers and
 * non-normal distributions, which suits noisy GC-log data well. Theil-Sen
 * is the matching robust slope estimator.
 *
 * Why post-GC and not post-Full-GC: modern collectors (G1, ZGC, Shenandoah)
 * may run for hours without a Full GC, so restricting to Full GCs would
 * leave most logs without a verdict. The trade-off: post-young-GC samples
 * include short-lived garbage that has been promoted but not yet reclaimed,
 * which adds noise. Mann-Kendall tolerates that noise, and the rank-based
 * test still surfaces a true monotonic trend underneath it.
 */
public class HeapStabilitySummary extends HeapStabilityAggregation {

    /** Below this many post-GC samples the trend test is meaningless. */
    private static final int MIN_SAMPLES = 20;

    /**
     * Below this much wall-clock data we won't issue a leak verdict at all.
     * The first chunk of any JVM run is dominated by class loading, JIT
     * compilation, and heap committing — patterns that look identical to a
     * leak to a naive trend test. 30 minutes gives the JVM enough time to
     * settle past warmup and reach a representative steady state.
     */
    private static final double MIN_SPAN_SECONDS = 1800.0;

    /**
     * Practical-significance gate: a verdict above STABLE requires the slope
     * to clear BOTH an absolute floor (MB/h) and a relative floor (%/h of
     * mean post-GC heap). Mann-Kendall's z-score scales with √n, so on a
     * long run with thousands of samples even a vanishingly small trend
     * hits z &gt; 3 — without this gate, the test reports leaks that aren't
     * operationally meaningful.
     */
    private static final double MIN_LEAK_MB_PER_HOUR  = 1.0;
    private static final double MIN_LEAK_PCT_PER_HOUR = 1.0;

    /**
     * Pairwise-slope work for Theil-Sen is O(n²). With n=5000 that's ~12.5M
     * slopes — sub-second in Java and ~100MB of doubles, which is fine. Larger
     * inputs are evenly downsampled before the slope is computed. The
     * Mann-Kendall S statistic still uses every sample.
     */
    private static final int SLOPE_SAMPLE_CAP = 5000;

    /** |z| thresholds for two-sided 95%, 99%, 99.9% confidence. */
    private static final double Z_95  = 1.9600;
    private static final double Z_99  = 2.5758;
    private static final double Z_999 = 3.2905;

    private final DoubleArray times = new DoubleArray();
    private final LongArray   heaps = new LongArray();

    private Verdict cachedVerdict;   // computed lazily; invalidated on each new sample

    @Override
    public void recordCollection(double timeSeconds, long heapAfterKB) {
        times.add(timeSeconds);
        heaps.add(heapAfterKB);
        cachedVerdict = null;
    }

    @Override
    public boolean hasWarning() {
        return verdict().suspicious;
    }

    @Override
    public boolean isEmpty() {
        return heaps.size() == 0;
    }

    public void printOn(PrintStream out, double runtimeDuration) {
        out.printf("Heap stability%n");
        int n = heaps.size();
        if (n < MIN_SAMPLES) {
            out.printf("  (need >= %d post-GC samples for a meaningful trend test; have %d)%n",
                    MIN_SAMPLES, n);
            return;
        }

        long[]   h = heaps.toArray();
        double[] t = times.toArray();
        double sampleSpan = t[t.length - 1] - t[0];

        // The "window" for display and the warmup gate is the JVM's reported
        // runtime — that's what the user thinks of as "how long is this log?".
        // Sample span is usually shorter (first GC fires some way into the
        // run, last GC stops before the log ends). Fall back to sample span
        // if runtime wasn't reported.
        double window = runtimeDuration > 0.0 ? runtimeDuration : sampleSpan;
        Verdict v = computeVerdict(window);

        out.printf("  Samples              : %d%n", n);
        out.printf("  Run duration         : %s%n", formatSpan(window));
        if (Math.abs(window - sampleSpan) > 1.0) {
            // GC events didn't bracket the whole run; worth surfacing so the
            // user knows what the test actually saw.
            out.printf("  Sample span          : %s%n", formatSpan(sampleSpan));
        }
        out.printf("  First / last post-GC : %d / %d MB%n",
                h[0] / 1024L, h[h.length - 1] / 1024L);
        out.printf("  Mann-Kendall S       : %d%n", v.stat);
        out.printf("  Mann-Kendall Z       : %+.2f%n", v.z);
        out.printf("  Two-sided p-value    : %.3g%n", v.pValue);
        out.printf("  Theil-Sen slope      : %s%n", formatSlope(v.slopeMBPerHour));
        out.printf("  Verdict              : %s%n", v.label);
    }

    /** Seconds if &lt;120s, else minutes if &lt;2h, else hours. Two significant digits. */
    static String formatSpan(double seconds) {
        if (seconds < 120.0) return String.format("%.1f s", seconds);
        if (seconds < 7200.0) return String.format("%.1f min", seconds / 60.0);
        return String.format("%.2f h", seconds / 3600.0);
    }

    /**
     * Slopes near zero are what the practical-significance gate is for — show
     * them with enough precision that "+0.00 MB/h" can't appear alongside a
     * leak verdict and look contradictory.
     */
    static String formatSlope(double mbPerHour) {
        double a = Math.abs(mbPerHour);
        if (a >= 100.0) return String.format("%+.1f MB/h", mbPerHour);
        if (a >= 1.0)   return String.format("%+.2f MB/h", mbPerHour);
        if (a >= 0.01)  return String.format("%+.3f MB/h", mbPerHour);
        return String.format("%+.2e MB/h", mbPerHour);
    }

    // --- Verdict computation ---------------------------------------------------

    /**
     * Used by hasWarning(), which has no access to the JVM runtime. Falls back
     * to sample span, which is always &lt;= runtime, so this can only be more
     * conservative (a "window too short" verdict here might flip to a leak
     * verdict once printOn() runs with the true runtime — fine, since
     * hasWarning is only advisory).
     */
    private Verdict verdict() {
        if (cachedVerdict != null) return cachedVerdict;
        int n = times.size();
        double[] t = times.toArray();
        double span = n >= 2 ? t[n - 1] - t[0] : 0.0;
        cachedVerdict = computeVerdict(span);
        return cachedVerdict;
    }

    private Verdict computeVerdict(double windowSeconds) {
        int n = heaps.size();
        if (n < MIN_SAMPLES) {
            return new Verdict(0L, 0.0, 1.0, 0.0, "INSUFFICIENT DATA", false);
        }
        long[]   h = heaps.toArray();
        double[] t = times.toArray();

        long S        = mannKendallS(h);
        double varS   = varianceOfS(n, tieGroupSizes(h));
        double z      = zStat(S, varS);
        double p      = twoSidedPValue(z);
        double slope  = theilSenSlopeKBPerSec(t, h) * 3600.0 / 1024.0;  // MB/h

        // Refuse to issue a leak verdict on a window dominated by warmup.
        // Below MIN_SPAN_SECONDS, any trend is more plausibly attributable to
        // class loading, JIT compilation, and heap commitment than to a leak.
        if (windowSeconds < MIN_SPAN_SECONDS) {
            String msg = String.format("INSUFFICIENT WINDOW (%s; need >= %d min for a leak verdict)",
                    formatSpan(windowSeconds), (int) (MIN_SPAN_SECONDS / 60));
            return new Verdict(S, z, p, slope, msg, false);
        }

        return new Verdict(S, z, p, slope, label(z, slope, h), isLeak(z, slope, h));
    }

    /**
     * A leak verdict requires both statistical significance (z &gt;= Z_95) and
     * practical significance (slope clears both an absolute floor and a
     * floor relative to the mean post-GC heap). Either alone misleads.
     */
    private static boolean isLeak(double z, double slopeMBPerHour, long[] h) {
        return z >= Z_95 && isPracticallySignificant(slopeMBPerHour, h);
    }

    private static boolean isPracticallySignificant(double slopeMBPerHour, long[] h) {
        if (slopeMBPerHour <= 0.0) return false;
        double meanMB = meanKB(h) / 1024.0;
        if (meanMB <= 0.0) return slopeMBPerHour >= MIN_LEAK_MB_PER_HOUR;
        double pctPerHour = (slopeMBPerHour / meanMB) * 100.0;
        return slopeMBPerHour >= MIN_LEAK_MB_PER_HOUR
            && pctPerHour      >= MIN_LEAK_PCT_PER_HOUR;
    }

    private static double meanKB(long[] h) {
        double sum = 0.0;
        for (long v : h) sum += v;
        return sum / h.length;
    }

    private static String label(double z, double slopeMBPerHour, long[] h) {
        // Negative or non-significant trend is not a leak signal. We only flag
        // statistically significant *upward* drift.
        if (z < Z_95) return "STABLE";

        // Statistically significant rising trend, but slope is too small to
        // matter operationally. Common with very long, well-behaved runs.
        if (!isPracticallySignificant(slopeMBPerHour, h)) {
            return String.format(
                "STABLE (significant but negligible drift: %s)",
                formatSlope(slopeMBPerHour));
        }

        if (z < Z_99)  return String.format("GROWING (%s)", formatSlope(slopeMBPerHour));
        if (z < Z_999) return String.format("LEAK SUSPECTED (%s)", formatSlope(slopeMBPerHour));
        return                 String.format("STRONG LEAK SIGNAL (%s)", formatSlope(slopeMBPerHour));
    }

    // --- Mann-Kendall ----------------------------------------------------------

    /**
     * S = Σ_{i<j} sign(x_j - x_i). Positive S means values trend up over the
     * sequence. O(n²) time, O(1) extra space.
     */
    static long mannKendallS(long[] x) {
        long S = 0;
        for (int i = 0; i < x.length; i++) {
            long xi = x[i];
            for (int j = i + 1; j < x.length; j++) {
                long d = x[j] - xi;
                if      (d > 0) S++;
                else if (d < 0) S--;
            }
        }
        return S;
    }

    /** Sizes of tie groups (values that occur 2+ times). */
    static int[] tieGroupSizes(long[] x) {
        Map<Long, Integer> counts = new HashMap<>();
        for (long v : x) counts.merge(v, 1, Integer::sum);
        int distinctTied = 0;
        for (int c : counts.values()) if (c >= 2) distinctTied++;
        int[] out = new int[distinctTied];
        int i = 0;
        for (int c : counts.values()) if (c >= 2) out[i++] = c;
        return out;
    }

    /**
     * Var(S) under H0 with tie correction:
     *   Var(S) = [ n(n-1)(2n+5) - Σ t_i(t_i-1)(2t_i+5) ] / 18
     */
    static double varianceOfS(int n, int[] tieGroups) {
        double base = (double) n * (n - 1) * (2L * n + 5);
        double tieCorrection = 0.0;
        for (int t : tieGroups) {
            tieCorrection += (double) t * (t - 1) * (2L * t + 5);
        }
        return (base - tieCorrection) / 18.0;
    }

    /**
     * Standardised statistic using the continuity correction that bumps S
     * one toward zero. With Var(S)<=0 (n=1 or all values tied) we treat the
     * trend as undefined — return 0.
     */
    static double zStat(long S, double varS) {
        if (varS <= 0.0) return 0.0;
        double sd = Math.sqrt(varS);
        if (S > 0) return (S - 1) / sd;
        if (S < 0) return (S + 1) / sd;
        return 0.0;
    }

    /**
     * Two-sided p-value from the standard normal: p = 1 - erf(|z|/√2).
     *
     * erf approximation: Abramowitz & Stegun 7.1.26, |error| < 1.5e-7. This
     * is more than precise enough for the significance bands we report on.
     */
    static double twoSidedPValue(double z) {
        double a = Math.abs(z) / Math.sqrt(2.0);
        double t = 1.0 / (1.0 + 0.3275911 * a);
        double poly = ((((1.061405429 * t
                       - 1.453152027) * t
                       + 1.421413741) * t
                       - 0.284496736) * t
                       + 0.254829592) * t;
        double erf = 1.0 - poly * Math.exp(-a * a);
        return Math.max(0.0, Math.min(1.0, 1.0 - erf));
    }

    // --- Theil-Sen slope --------------------------------------------------------

    /**
     * Median of all pairwise slopes (h_j - h_i) / (t_j - t_i). Same-time
     * pairs are skipped. For very long series we evenly downsample the
     * indices to bound the O(n²) work.
     */
    static double theilSenSlopeKBPerSec(double[] t, long[] h) {
        int[] idx = downsampleIndices(h.length, SLOPE_SAMPLE_CAP);
        int m = idx.length;
        double[] slopes = new double[m * (m - 1) / 2];
        int k = 0;
        for (int i = 0; i < m; i++) {
            double ti = t[idx[i]];
            long   hi = h[idx[i]];
            for (int j = i + 1; j < m; j++) {
                double dt = t[idx[j]] - ti;
                if (dt == 0.0) continue;   // simultaneous samples — undefined slope
                slopes[k++] = (h[idx[j]] - hi) / dt;
            }
        }
        if (k == 0) return 0.0;
        double[] s = (k == slopes.length) ? slopes : Arrays.copyOf(slopes, k);
        Arrays.sort(s);
        return (k % 2 == 1) ? s[k / 2] : 0.5 * (s[k / 2 - 1] + s[k / 2]);
    }

    /** Evenly-spaced indices, endpoints inclusive. Identity if n <= cap. */
    static int[] downsampleIndices(int n, int cap) {
        if (n <= cap) {
            int[] all = new int[n];
            for (int i = 0; i < n; i++) all[i] = i;
            return all;
        }
        int[] out = new int[cap];
        for (int i = 0; i < cap; i++) {
            out[i] = (int) Math.round((double) i * (n - 1) / (cap - 1));
        }
        return out;
    }

    // --- Verdict + small primitive arrays ---------------------------------------

    private record Verdict(long stat, double z, double pValue,
                           double slopeMBPerHour, String label, boolean suspicious) {}

    /** Auto-growing double[]; avoids the boxing overhead of ArrayList<Double>. */
    private static final class DoubleArray {
        private double[] data = new double[64];
        private int size = 0;
        void add(double v) {
            if (size == data.length) data = Arrays.copyOf(data, size * 2);
            data[size++] = v;
        }
        double[] toArray() { return Arrays.copyOf(data, size); }
        int size() { return size; }
    }

    /** Auto-growing long[]; same rationale. */
    private static final class LongArray {
        private long[] data = new long[64];
        private int size = 0;
        void add(long v) {
            if (size == data.length) data = Arrays.copyOf(data, size * 2);
            data[size++] = v;
        }
        long[] toArray() { return Arrays.copyOf(data, size); }
        int size() { return size; }
    }
}
