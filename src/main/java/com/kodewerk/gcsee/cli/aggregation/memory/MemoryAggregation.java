package com.kodewerk.gcsee.cli.aggregation.memory;

import com.kodewerk.gcsee.aggregator.Aggregation;
import com.kodewerk.gcsee.aggregator.Collates;

@Collates(MemoryAggregator.class)
public abstract class MemoryAggregation extends Aggregation {

    /**
     * Record the heap occupancy immediately before and after a collection.
     *
     * @param heapBeforeKB heap occupancy before collection, in KB
     * @param heapAfterKB  heap occupancy after collection, in KB
     */
    public abstract void recordCollection(long heapBeforeKB, long heapAfterKB);
}
