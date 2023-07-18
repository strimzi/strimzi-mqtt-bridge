/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

/**
 * Represents the result of a mapping operation.
 * It contains the mapped Kafka topic and the Kafka key.
 *
 * @param kafkaTopic the mapped Kafka topic.
 * @param kafkaKey   the Kafka key.
 */
public record MappingResult(String kafkaTopic, String kafkaKey) {

    @Override
    public String toString() {
        return "MappingResult(" +
                "kafkaTopic=" + kafkaTopic +
                ", kafkaKey=" + kafkaKey +
                ")";
    }
}
