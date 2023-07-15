/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

/**
 * Represents the result of a mapping operation.
 * It contains the mapped kafka topic and the kafka key.
 */
public class MappingResult {
    private String kafkaTopic;
    private String kafkaKey;

    /**
     * Constructor for MappingResult.
     *
     * @param kafkaTopic the mapped kafka topic.
     * @param kafkaKey   the kafka key.
     */
    public MappingResult(String kafkaTopic, String kafkaKey) {
        this.kafkaTopic = kafkaTopic;
        this.kafkaKey = kafkaKey;
    }

    /**
     * Get the mapped kafka topic.
     *
     * @return the mapped kafka topic.
     */
    public String getKafkaTopic() {
        return kafkaTopic;
    }

    /**
     * Set the mapped kafka topic.
     *
     * @param mappedKafkaTopic the mapped kafka topic.
     */
    public void setKafkaTopic(String mappedKafkaTopic) {
        this.kafkaTopic = mappedKafkaTopic;
    }

    /**
     * Get the kafka key.
     *
     * @return the kafka key.
     */
    public String getKafkaKey() {
        return kafkaKey;
    }

    /**
     * Set the kafka key.
     *
     * @param kafkaKey the kafka key.
     */
    public void setKafkaKey(String kafkaKey) {
        this.kafkaKey = kafkaKey;
    }

    @Override
    public String toString() {
        return "MappingResult(kafkaTopic= "
                + this.kafkaTopic
                + ", kafkaKey=" + this.kafkaKey + ")";
    }
}
