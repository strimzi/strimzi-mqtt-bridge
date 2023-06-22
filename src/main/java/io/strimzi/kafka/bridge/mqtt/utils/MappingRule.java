/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.utils;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Mapping Rule in the Topic Mapping Rules(TOMAR). Mapping rules are used to define how MQTT topics should be mapped to Kafka topics.
 * E.g.: a valid mapping rule would look like this in the TOMAR file:
 * {
 * "kafkaTopic": "sensors_{sensorId}_data",
 * "mqttTopic": "sensors/{sensorId}/data"
 * }
 * and like this in the MappingRule class:
 * MappingRule(kafkaTopicTemplate=sensors_{sensorId}_data mqttTopicPattern= sensors/{sensorId}/data)
 */
public class MappingRule {
    @JsonProperty("kafkaTopic")
    private String kafkaTopicTemplate;
    @JsonProperty("mqttTopic")
    private String mqttTopicPattern;

    /**
     * Default constructor for MappingRule. Used for deserialization.
     */
    public MappingRule() {
    }

    /**
     * Constructor for MappingRule.
     *
     * @param kafkaTopicTemplate the kafka topic template.
     * @param mqttTopicPattern   the mqtt topic pattern.
     */
    public MappingRule(String mqttTopicPattern, String kafkaTopicTemplate) {
        this.kafkaTopicTemplate = kafkaTopicTemplate;
        this.mqttTopicPattern = mqttTopicPattern;
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
     * String representation of a MappingRule.
     *
     * @return a string containing properties of a MappingRule.
     */
    @Override
    public String toString() {
        return "MappingRule(kafkaTopic=" + this.kafkaTopicTemplate + " mqttTopic= " + this.mqttTopicPattern + ")";
    }
}
