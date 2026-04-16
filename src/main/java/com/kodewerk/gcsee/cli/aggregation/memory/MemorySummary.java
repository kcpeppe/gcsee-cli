package com.kodewerk.gcsee.cli.aggregation.memory;

import java.io.PrintStream;

public class MemorySummary extends MemoryAggregation {

    private long totalRecoveredKB = 0L;
    private long totalAllocatedKB = 0L;
    private long previousHeapAfterKB = -1L;

    @Override
    public void recordCollection(long heapBeforeKB, long heapAfterKB) {
        // Memory freed by this collection
        totalRecoveredKB += Math.max(0, heapBeforeKB - heapAfterKB);

        // Memory allocated since the previous collection
        if (previousHeapAfterKB >= 0) {
            totalAllocatedKB += Math.max(0, heapBeforeKB - previousHeapAfterKB);
        }

        previousHeapAfterKB = heapAfterKB;
    }

    public long getMBRecovered() {
        return totalRecoveredKB / 1024L;
    }

    public double getMBRecoveredPerSecond(double runtimeDuration) {
        return runtimeDuration > 0.0 ? getMBRecovered() / runtimeDuration : 0.0;
    }

    public double getAllocationRateMBPerSecond(double runtimeDuration) {
        return runtimeDuration > 0.0 ? (totalAllocatedKB / 1024.0) / runtimeDuration : 0.0;
    }

    @Override
    public boolean hasWarning() { return false; }

    @Override
    public boolean isEmpty() { return totalRecoveredKB == 0L; }

    public void printOn(PrintStream out, double runtimeDuration) {
        out.printf("MB recovered      : %d%n",        getMBRecovered());
        out.printf("MB recovered/s    : %.2f%n",      getMBRecoveredPerSecond(runtimeDuration));
        out.printf("Allocation rate   : %.2f MB/s%n", getAllocationRateMBPerSecond(runtimeDuration));
    }
}
