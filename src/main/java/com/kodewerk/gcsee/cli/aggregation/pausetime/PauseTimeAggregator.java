package com.kodewerk.gcsee.cli.aggregation.pausetime;

import com.kodewerk.gcsee.aggregator.Aggregates;
import com.kodewerk.gcsee.aggregator.Aggregator;
import com.kodewerk.gcsee.aggregator.EventSource;
import com.kodewerk.gcsee.event.g1gc.G1GCPauseEvent;

// TODO: extend to GENERATIONAL, ZGC, SHENANDOAH
@Aggregates({EventSource.G1GC})
public class PauseTimeAggregator extends Aggregator<PauseTimeAggregation> {

    public PauseTimeAggregator(PauseTimeAggregation aggregation) {
        super(aggregation);
        register(G1GCPauseEvent.class, this::process);
    }

    private void process(G1GCPauseEvent event) {
        if ( event.getDuration() < 0.0d) {
            System.out.println("Event missing pause duration @" + event.getDateTimeStamp().toSeconds());
        }
        aggregation().recordPauseDuration(event.getDuration());
    }
}
