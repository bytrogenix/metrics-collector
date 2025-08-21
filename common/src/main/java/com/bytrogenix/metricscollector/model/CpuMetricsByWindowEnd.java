package com.bytrogenix.metricscollector.model;

import com.datastax.driver.mapping.annotations.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Table(keyspace = "local", name = "cpu_metrics_by_window_end")
@Data
@Accessors(chain = true)
public class CpuMetricsByWindowEnd {
    private String deviceId;
    private Date windowEnd;
    private Date windowStart;
    private Double percentile95;
    private Date lastUpdated;
}
