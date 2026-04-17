package com.kodewerk.gcsee.cli;

import com.kodewerk.gcsee.GCSee;
import com.kodewerk.gcsee.io.GCLogFile;
import com.kodewerk.gcsee.jvm.JavaVirtualMachine;
import com.kodewerk.gcsee.cli.aggregation.heapstability.HeapStabilitySummary;
import com.kodewerk.gcsee.cli.aggregation.memory.MemorySummary;
import com.kodewerk.gcsee.cli.aggregation.pausetime.PauseTimeSummary;

import java.io.IOException;

/**
 * Runs a GCSee analysis on a log file and prints the results.
 * Shared by both batch and interactive modes.
 */
public class Analysis {

    public void run(GCLogFile logFile) throws IOException {
        GCSee gcSee = new GCSee();
        gcSee.loadAggregationsFromServiceLoader();

        JavaVirtualMachine jvm = gcSee.analyze(logFile);
        double runtime = jvm.getRuntimeDuration();

        jvm.getAggregation(PauseTimeSummary.class)
           .ifPresent(s -> s.printOn(System.out, runtime));

        jvm.getAggregation(MemorySummary.class)
           .ifPresent(s -> s.printOn(System.out, runtime));

        jvm.getAggregation(HeapStabilitySummary.class)
           .ifPresent(s -> s.printOn(System.out, runtime));
    }
}
