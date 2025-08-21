package com.bytrogenix.metricscollector.db.repository;

import com.bytrogenix.metricscollector.model.CpuMetricsByDevice;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

import java.util.Optional;

public interface CpuMetricsByDeviceRepository extends CassandraRepository<CpuMetricsByDevice, String> {

    @Query("SELECT * FROM cpu_metrics_by_device WHERE deviceId = :deviceId ORDER BY windowEnd DESC LIMIT 1")
    Optional<CpuMetricsByDevice> findLatestByDeviceId(String deviceId);
}
