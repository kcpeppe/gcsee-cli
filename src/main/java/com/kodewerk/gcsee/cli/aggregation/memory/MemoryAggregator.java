package com.kodewerk.gcsee.cli.aggregation.memory;

import com.kodewerk.gcsee.aggregator.Aggregates;
import com.kodewerk.gcsee.aggregator.Aggregator;
import com.kodewerk.gcsee.aggregator.EventSource;
import com.kodewerk.gcsee.event.g1gc.G1Cleanup;
import com.kodewerk.gcsee.event.g1gc.G1FullGC;
import com.kodewerk.gcsee.event.g1gc.G1Remark;
import com.kodewerk.gcsee.event.g1gc.G1Young;
import com.kodewerk.gcsee.event.generational.GenerationalGCPauseEvent;
import com.kodewerk.gcsee.event.zgc.ZGCFullCollection;
import com.kodewerk.gcsee.event.zgc.ZGCOldCollection;
import com.kodewerk.gcsee.event.zgc.ZGCYoungCollection;

@Aggregates({EventSource.G1GC, EventSource.GENERATIONAL, EventSource.ZGC})
public class MemoryAggregator extends Aggregator<MemoryAggregation> {

    public MemoryAggregator(MemoryAggregation aggregation) {
        super(aggregation);
        // G1GC: register only the concrete types that populate heap data.
        // G1Young covers G1Mixed and G1YoungInitialMark via the event hierarchy.
        // G1FullGC covers G1FullGCNES and G1SystemGC.
        register(G1Young.class,   this::extract);
        register(G1FullGC.class,  this::extract);
        register(G1Cleanup.class, this::extract);
        register(G1Remark.class,  this::extract);
        register(GenerationalGCPauseEvent.class, this::extract);
        register(ZGCYoungCollection.class, this::extract);
        register(ZGCOldCollection.class,   this::extract);
        register(ZGCFullCollection.class,  this::extract);
        // TODO: ShenandoahCycle — no heap occupancy API exposed
    }

    private void extract(G1Young event) {
        aggregation().recordCollection(
                event.getHeap().getOccupancyBeforeCollection(),
                event.getHeap().getOccupancyAfterCollection());
    }

    private void extract(G1FullGC event) {
        aggregation().recordCollection(
                event.getHeap().getOccupancyBeforeCollection(),
                event.getHeap().getOccupancyAfterCollection());
    }

    private void extract(G1Cleanup event) {
        aggregation().recordCollection(
                event.getHeap().getOccupancyBeforeCollection(),
                event.getHeap().getOccupancyAfterCollection());
    }

    private void extract(G1Remark event) {
        aggregation().recordCollection(
                event.getHeap().getOccupancyBeforeCollection(),
                event.getHeap().getOccupancyAfterCollection());
    }

    private void extract(GenerationalGCPauseEvent event) {
        try {
            aggregation().recordCollection(
                    event.getHeap().getOccupancyBeforeCollection(),
                    event.getHeap().getOccupancyAfterCollection());
        } catch (NullPointerException npe) {}
    }

    private void extract(ZGCYoungCollection event) {
        aggregation().recordCollection(
                event.getMemorySummary().getOccupancyBefore(),
                event.getMemorySummary().getOccupancyAfter());
    }

    private void extract(ZGCOldCollection event) {
        aggregation().recordCollection(
                event.getMemorySummary().getOccupancyBefore(),
                event.getMemorySummary().getOccupancyAfter());
    }

    private void extract(ZGCFullCollection event) {
        aggregation().recordCollection(
                event.getMemorySummary().getOccupancyBefore(),
                event.getMemorySummary().getOccupancyAfter());
    }
}
