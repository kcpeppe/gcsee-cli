package com.kodewerk.gcsee.cli.aggregation.pausetime;

import java.io.PrintStream;

public class PauseTimeSummary extends PauseTimeAggregation {

    private double totalPauseTime = 0.0;

    @Override
    public void recordPauseDuration(double duration) {
        totalPauseTime += duration;
    }

    public double getTotalPauseTime() {
        return totalPauseTime;
    }

    public double getPercentPaused() {
        double runtime = estimatedRuntime();
        return runtime > 0.0 ? (totalPauseTime / runtime) * 100.0 : 0.0;
    }

    @Override
    public boolean hasWarning() { return false; }

    @Override
    public boolean isEmpty() { return totalPauseTime == 0.0; }

    public void printOn(PrintStream out, double runtimeDuration) {
        out.printf("Total pause time  : %.4f%n", totalPauseTime);
        out.printf("Total run time    : %.4f%n", runtimeDuration);
        out.printf("Percent pause time: %.2f%%%n", getPercentPaused());
    }
}
