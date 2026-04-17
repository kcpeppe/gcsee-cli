package com.kodewerk.gcsee.cli.aggregation.heapstability;

import com.kodewerk.gcsee.aggregator.Aggregation;
import com.kodewerk.gcsee.aggregator.Collates;

@Collates(HeapStabilityAggregator.class)
public abstract class HeapStabilityAggregation extends Aggregation {

    /**
     * Record the heap occupancy immediately after a collection at a given
     * point in the run.
     *
     * @param timeSeconds wall-clock-relative time of the collection, in seconds
     * @param heapAfterKB heap occupancy immediately after collection, in KB
     */
    public abstract void recordCollection(double timeSeconds, long heapAfterKB);
}
