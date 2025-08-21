package com.bytrogenix.metricscollector.kafka;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Accessors(chain = true)
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfigProperties {

    private Topic topic;

    @Data
    @Accessors(chain = true)
    public static class Topic {
        private String deviceEvents;
    }
}
