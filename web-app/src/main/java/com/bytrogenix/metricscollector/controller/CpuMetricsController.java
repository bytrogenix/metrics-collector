package com.bytrogenix.metricscollector.controller;

import com.bytrogenix.metricscollector.db.repository.CpuMetricsByDeviceRepository;
import com.bytrogenix.metricscollector.db.repository.CpuMetricsByWindowEndRepository;
import com.bytrogenix.metricscollector.model.CpuMetricsByDevice;
import com.bytrogenix.metricscollector.model.CpuMetricsByWindowEnd;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/metrics")
@AllArgsConstructor
public class CpuMetricsController {

    private final CpuMetricsByDeviceRepository metricsByDeviceRepository;
    private final CpuMetricsByWindowEndRepository metricsByWindowEndRepository;

    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<CpuMetricsByDevice> getDeviceMetrics(@PathVariable String deviceId) {
        var metrics = metricsByDeviceRepository.findLatestByDeviceId(deviceId);

        return metrics
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/top/{n:\\d+}")
    public ResponseEntity<List<CpuMetricsByWindowEnd>> getTopDevices(@PathVariable int n) {
        var latestWindowEnd = metricsByWindowEndRepository.findLatestWindowEnd();
        if (latestWindowEnd.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        //TODO: this will cause out of memory exception, should be solved by CDC into proper storage (Elasticsearch)
        var latestMetrics = metricsByWindowEndRepository.findLatestMetrics(latestWindowEnd.get());
        var result = latestMetrics.stream()
            .sorted(Comparator.comparingDouble(CpuMetricsByWindowEnd::getPercentile95).reversed())
            .limit(n)
            .toList();

        return ResponseEntity.ok(result);
    }
}
