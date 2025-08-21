package com.bytrogenix.metricscollector.fake;

import com.bytrogenix.metricscollector.kafka.KafkaConfigProperties;
import com.bytrogenix.metricscollector.model.DeviceEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
public class DeviceEventGenerator {
    private final KafkaTemplate<String, DeviceEvent> kafkaTemplate;
    private final KafkaConfigProperties kafkaConfigProperties;

    public DeviceEventGenerator(
        KafkaTemplate<String, DeviceEvent> kafkaTemplate,
        KafkaConfigProperties kafkaConfigProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaConfigProperties = kafkaConfigProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        log.info("Starting DeviceEventGenerator...");

        // in-time events ~10 events/sec
        Thread normalEvents = new Thread(() -> produceEvents(0, 100));
        normalEvents.setDaemon(true);
        normalEvents.start();

        // very late events ~1 event/sec
        Thread delayedEvents = new Thread(() -> produceEvents(15*1000*60, 1000));
        delayedEvents.setDaemon(true);
        delayedEvents.start();

        log.info("DeviceEventGenerator started");
    }

    @SneakyThrows
    private void produceEvents(long timestampDelayMs, long produceDelayMs) {
        var rand = new Random();
        while (true) {
            var deviceId = "device-" + rand.nextInt(50);
            var timestamp = System.currentTimeMillis() + timestampDelayMs;
            var cpuUsage = ThreadLocalRandom.current().nextDouble(0, 100);

            kafkaTemplate.send(
                kafkaConfigProperties.getTopic().getDeviceEvents(),
                deviceId,
                new DeviceEvent(deviceId, timestamp, cpuUsage)
            );

            Thread.sleep(produceDelayMs);
        }
    }
}
