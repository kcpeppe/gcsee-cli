package com.kodewerk.gcsee.cli.aggregation.heapstability;

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

/**
 * Pumps post-collection heap occupancy plus event time into the
 * HeapStabilityAggregation. Mirrors the event coverage of MemoryAggregator;
 * we deliberately read the same set of events so the trend test sees the
 * same population that the throughput summary sees.
 */
@Aggregates({EventSource.G1GC, EventSource.GENERATIONAL, EventSource.ZGC})
public class HeapStabilityAggregator extends Aggregator<HeapStabilityAggregation> {

    public HeapStabilityAggregator(HeapStabilityAggregation aggregation) {
        super(aggregation);
        register(G1Young.class,   this::extract);
        register(G1FullGC.class,  this::extract);
        register(G1Cleanup.class, this::extract);
        register(G1Remark.class,  this::extract);
        register(GenerationalGCPauseEvent.class, this::extract);
        register(ZGCYoungCollection.class, this::extract);
        register(ZGCOldCollection.class,   this::extract);
        register(ZGCFullCollection.class,  this::extract);
    }

    private void extract(G1Young event) {
        aggregation().recordCollection(
                event.getDateTimeStamp().toSeconds(),
                event.getHeap().getOccupancyAfterCollection());
    }

    private void extract(G1FullGC event) {
        aggregation().recordCollection(
                event.getDateTimeStamp().toSeconds(),
                event.getHeap().getOccupancyAfterCollection());
    }

    private void extract(G1Cleanup event) {
        aggregation().recordCollection(
                event.getDateTimeStamp().toSeconds(),
                event.getHeap().getOccupancyAfterCollection());
    }

    private void extract(G1Remark event) {
        aggregation().recordCollection(
                event.getDateTimeStamp().toSeconds(),
                event.getHeap().getOccupancyAfterCollection());
    }

    private void extract(GenerationalGCPauseEvent event) {
        try {
            aggregation().recordCollection(
                    event.getDateTimeStamp().toSeconds(),
                    event.getHeap().getOccupancyAfterCollection());
        } catch (NullPointerException npe) {
            // Mirrors MemoryAggregator: some generational events ship without a heap section.
        }
    }

    private void extract(ZGCYoungCollection event) {
        aggregation().recordCollection(
                event.getDateTimeStamp().toSeconds(),
                event.getMemorySummary().getOccupancyAfter());
    }

    private void extract(ZGCOldCollection event) {
        aggregation().recordCollection(
                event.getDateTimeStamp().toSeconds(),
                event.getMemorySummary().getOccupancyAfter());
    }

    private void extract(ZGCFullCollection event) {
        aggregation().recordCollection(
                event.getDateTimeStamp().toSeconds(),
                event.getMemorySummary().getOccupancyAfter());
    }
}
