/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents configurations related to Kafka
 * @see AbstractConfig
 */
public class KafkaProducerConfig extends AbstractConfig {

    // Prefix for all the specific configuration parameters for Kafka producer in the properties file
    public static final String KAFKA_PRODUCER_CONFIG_PREFIX = KafkaConfig.KAFKA_CONFIG_PREFIX + "producer.";

    /**
     * Constructor
     *
     * @param config configuration parameters map
     */
    public KafkaProducerConfig(Map<String, Object> config) {
        super(config);
    }

    /**
     * Build a Kafka producer configuration object from a map of configuration parameters
     *
     * @param map configuration parameters map
     * @return a new instance of KafkaProducerConfig
     */
    public static KafkaProducerConfig fromMap(Map<String, Object> map) {
        return new KafkaProducerConfig(map.entrySet().stream()
                .filter(e -> e.getKey().startsWith(KafkaProducerConfig.KAFKA_PRODUCER_CONFIG_PREFIX))
                .collect(Collectors.toMap(e -> e.getKey().substring(KafkaProducerConfig.KAFKA_PRODUCER_CONFIG_PREFIX.length()), Map.Entry::getValue)));
    }

    @Override
    public String toString() {
        return "KafkaProducerConfig(" +
                "config=" + config +
                ')';
    }
}
