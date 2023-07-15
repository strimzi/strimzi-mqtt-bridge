/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Mapping Rule in the Topic Mapping Rules(TOMAR). Mapping rules are used to define how MQTT topics should be mapped to Kafka topics.
 * E.g.: a valid mapping rule would look like this in the TOMAR file:
 * {
 *      "mqttTopic": "sensors/{sensorId}/data",
 *      "kafkaTopic": "sensors_{sensorId}_data"
 * }
 * and like this in the MappingRule class:
 * MappingRule(mqttTopicPattern= sensors/{sensorId}/data, kafkaTopicTemplate=sensors_{sensorId}_data)
 */
public class MappingRule {
    @JsonProperty("mqttTopic")
    private String mqttTopicPattern;
    @JsonProperty("kafkaTopic")
    private String kafkaTopicTemplate;

    @JsonProperty("kafkaKey")
    private String kafkaKey;

    /**
     * Default constructor for MappingRule. Used for deserialization.
     */
    public MappingRule() {
    }

    /**
     * Constructor for MappingRule.
     *
     * @param mqttTopicPattern   the mqtt topic pattern.
     * @param kafkaTopicTemplate the kafka topic template.
     */
    public MappingRule(String mqttTopicPattern, String kafkaTopicTemplate, String kafkaKey) {
        this.mqttTopicPattern = mqttTopicPattern;
        this.kafkaTopicTemplate = kafkaTopicTemplate;
        this.kafkaKey = kafkaKey;
    }

    /**
     * Get the kafka topic template.
     *
     * @return the kafka topic template.
     */
    public String getKafkaTopicTemplate() {
        return kafkaTopicTemplate;
    }

    /**
     * Get the mqtt topic pattern.
     *
     * @return the mqtt topic pattern.
     */
    public String getMqttTopicPattern() {
        return mqttTopicPattern;
    }

    /**
     * Get the record key.
     *
     * @return the record key.
     */
    public String getKafkaKey() {
        return kafkaKey;
    }

    /**
     * String representation of a MappingRule.
     *
     * @return a string containing properties of a MappingRule.
     */
    @Override
    public String toString() {
        return "MappingRule(" +
                "mqttTopicPattern= " + this.mqttTopicPattern +
                ", kafkaTopicTemplate=" + this.kafkaTopicTemplate +
                ")";
    }
}
