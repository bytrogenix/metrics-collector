package com.bytrogenix.metricscollector;

import com.bytrogenix.metricscollector.job.FlinkExecutionEnvFactory;
import com.bytrogenix.metricscollector.job.TDigestAggregate;
import com.bytrogenix.metricscollector.job.TDigestProcessWindowFunction;
import com.bytrogenix.metricscollector.model.DeviceEvent;
import com.bytrogenix.metricscollector.model.mapper.CpuMetricsByWindowEndMapper;
import com.datastax.driver.mapping.Mapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.connectors.cassandra.CassandraSink;
import org.apache.flink.util.OutputTag;

import java.time.Duration;

/**
 * TODO: use proper config values injection instead of hardcoded strings
 */
public class CpuUsageCollectorJob {
    private final static OutputTag<DeviceEvent> TOO_LATE_TAG = new OutputTag<>(
        "too-late",
        TypeInformation.of(DeviceEvent.class)
    );

    public static void main(String[] args) throws Exception {
        var env = new FlinkExecutionEnvFactory().get();

        // consume from `iot.device.events.dev` kafka topic
        var source = KafkaSource.<DeviceEvent>builder()
            .setBootstrapServers("kafka:29092")
            .setTopics("iot.device.events.dev")
            .setGroupId("cpu_metrics_collector_dev")
            .setStartingOffsets(OffsetsInitializer.latest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema<>(DeviceEvent.class))
            .build();

        // allow out of order events to arrive with up to 5mins delay
        var watermarkStrategy = WatermarkStrategy.<DeviceEvent>forBoundedOutOfOrderness(Duration.ofMinutes(5))
            .withTimestampAssigner((e, ts) -> e.getTimestamp());

        var stream = env.fromSource(
            source,
            watermarkStrategy,
            "device-events-source"
        );

        // calc p95 over source stream
        var result = stream
            .keyBy(DeviceEvent::getDeviceId)
            .window(SlidingEventTimeWindows.of(Duration.ofMinutes(30), Duration.ofMinutes(1)))
            .allowedLateness(Duration.ofMinutes(5))
            .sideOutputLateData(TOO_LATE_TAG)
            .aggregate(new TDigestAggregate(), new TDigestProcessWindowFunction(0.95));

        // push very late events to dead-letter kafka topic
        result
            .getSideOutput(TOO_LATE_TAG)
            .sinkTo(
                KafkaSink.<DeviceEvent>builder()
                    .setBootstrapServers("kafka:29092")
                    .setRecordSerializer(
                        KafkaRecordSerializationSchema.<DeviceEvent>builder()
                            .setValueSerializationSchema(new JsonSerializationSchema<>())
                            .setTopic("iot.cpu.metrics.tooLate")
                            .build()
                    )
                    .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                    .build()
            );

        //TODO: all the sinks must be non-blocking async batch inserts, current implementation has blocking io

        // sink to `cpu_metrics_by_device` table
        CassandraSink.addSink(result)
            .setMapperOptions(() -> new Mapper.Option[]{Mapper.Option.saveNullFields(false)})
            .setHost("cassandra")
            .build();

        // sink to `cpu_metrics_by_window_end` table
        CassandraSink.addSink(result.map(CpuMetricsByWindowEndMapper::fromCpuMetricsByDevice))
            .setMapperOptions(() -> new Mapper.Option[]{Mapper.Option.saveNullFields(false)})
            .setHost("cassandra")
            .build();

        env.execute("cpu-usage-collector-job");
    }
}
