package com.kodewerk.gcsee.cli.aggregation.pausetime;

import com.kodewerk.gcsee.aggregator.Aggregation;
import com.kodewerk.gcsee.aggregator.Collates;

@Collates(PauseTimeAggregator.class)
public abstract class PauseTimeAggregation extends Aggregation {

    public abstract void recordPauseDuration(double duration);
}
