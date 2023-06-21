/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import java.util.Map;

public class KafkaProducerConfig extends AbstractConfig {

    public static final String KAFKA_PRODUCER_CONFIG_PREFIX = KafkaConfig.KAFKA_CONFIG_PREFIX + "producer.";
    public KafkaProducerConfig(Map<String, Object> config) {
        super(config);
    }

    public static KafkaProducerConfig fromMap(Map<String, Object> map) {
        return new KafkaProducerConfig(map);
    }

    @Override
    public String toString() {
        return "KafkaProducerConfig(" +
                "config=" + config +
                ')';
    }
}
