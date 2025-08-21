package com.bytrogenix.metricscollector.job;

import com.bytrogenix.metricscollector.model.CpuMetricsByDevice;
import com.tdunning.math.stats.TDigest;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Instant;
import java.util.Date;

public class TDigestProcessWindowFunction extends ProcessWindowFunction<TDigest, CpuMetricsByDevice, String, TimeWindow> {

    private final double percentileFraction;

    public TDigestProcessWindowFunction(double percentileFraction) {
        this.percentileFraction = percentileFraction;
    }

    @Override
    public void process(
        String deviceId,
        Context ctx,
        Iterable<TDigest> elements,
        Collector<CpuMetricsByDevice> collector
    ) {
        var digest = TDigest.createDigest(100);
        for (var e : elements) {
            digest.add(e);
        }
        var percentile = digest.quantile(percentileFraction);

        var metrics = new CpuMetricsByDevice()
            .setDeviceId(deviceId)
            .setWindowStart(new Date(ctx.window().getStart()))
            .setWindowEnd(new Date(ctx.window().getEnd()))
            .setPercentile95(percentile)
            .setLastUpdated(Date.from(Instant.now()));

        collector.collect(metrics);
    }
}
