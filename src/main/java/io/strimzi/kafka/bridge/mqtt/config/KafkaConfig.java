/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import java.util.Map;

public class KafkaConfig extends AbstractConfig {

    public static final String KAFKA_CONFIG_PREFIX = "kafka.";
    private final KafkaProducerConfig kafkaProducerConfig;

    public KafkaConfig(Map<String, Object> config, KafkaProducerConfig kafkaProducerConfig) {
        super(config);
        this.kafkaProducerConfig = kafkaProducerConfig;
    }

    public static KafkaConfig fromMap(Map<String, Object> config) {
        KafkaProducerConfig kafkaProducerConfig = KafkaProducerConfig.fromMap(config);
        return new KafkaConfig(config, kafkaProducerConfig);
    }

    public KafkaProducerConfig getKafkaProducerConfig() {
        return kafkaProducerConfig;
    }

    @Override
    public String toString() {
        return "KafkaConfig(" +
                "config=" + config +
                ", kafkaProducerConfig=" + kafkaProducerConfig +
                ')';
    }
}
