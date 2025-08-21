package com.bytrogenix.metricscollector.job;

import com.bytrogenix.metricscollector.model.DeviceEvent;
import com.tdunning.math.stats.TDigest;
import org.apache.flink.api.common.functions.AggregateFunction;

/**
 * Window can have a lot of data, so instead of collecting all events,
 * we collect one compressed TDigest per window per device
 */
public class TDigestAggregate implements AggregateFunction<DeviceEvent, TDigest, TDigest> {

    @Override
    public TDigest createAccumulator() {
        return TDigest.createDigest(100);
    }

    @Override
    public TDigest add(DeviceEvent value, TDigest accumulator) {
        accumulator.add(value.getCpuUsage());
        return accumulator;
    }

    @Override
    public TDigest getResult(TDigest accumulator) {
        return accumulator;
    }

    @Override
    public TDigest merge(TDigest a, TDigest b) {
        a.add(b);
        return a;
    }
}
