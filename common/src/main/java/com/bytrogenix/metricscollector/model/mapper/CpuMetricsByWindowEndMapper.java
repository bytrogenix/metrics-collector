package com.bytrogenix.metricscollector.model.mapper;

import com.bytrogenix.metricscollector.model.CpuMetricsByDevice;
import com.bytrogenix.metricscollector.model.CpuMetricsByWindowEnd;

public class CpuMetricsByWindowEndMapper {

    public static CpuMetricsByWindowEnd fromCpuMetricsByDevice(CpuMetricsByDevice metrics) {
        return new CpuMetricsByWindowEnd()
            .setDeviceId(metrics.getDeviceId())
            .setWindowStart(metrics.getWindowStart())
            .setWindowEnd(metrics.getWindowEnd())
            .setPercentile95(metrics.getPercentile95())
            .setLastUpdated(metrics.getLastUpdated());
    }
}
