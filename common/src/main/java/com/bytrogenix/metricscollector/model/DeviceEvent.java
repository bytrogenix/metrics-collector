package com.bytrogenix.metricscollector.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceEvent {
    private String deviceId;
    private Long timestamp;
    private Double cpuUsage;
}
