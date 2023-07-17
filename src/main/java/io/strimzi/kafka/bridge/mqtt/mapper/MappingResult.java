/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

/**
 * Represents the result of a mapping operation.
 * It contains the mapped kafka topic and the kafka key.
 *
 * @param kafkaTopic the mapped kafka topic.
 * @param kafkaKey   the kafka key.
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
