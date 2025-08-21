package com.bytrogenix.metricscollector.db.repository;

import com.bytrogenix.metricscollector.model.CpuMetricsByWindowEnd;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface CpuMetricsByWindowEndRepository extends CassandraRepository<CpuMetricsByWindowEnd, String> {

    @Query("SELECT * FROM cpu_metrics_by_window_end WHERE windowEnd = :lastWindowEnd")
    List<CpuMetricsByWindowEnd> findLatestMetrics(Date lastWindowEnd);

    @Query("SELECT max(windowEnd) FROM cpu_metrics_by_window_end")
    Optional<Date> findLatestWindowEnd();
}
