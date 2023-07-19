/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Mapping Rule in the Topic Mapping Rules(ToMaR). Mapping rules are used to define how MQTT topics should be mapped to Kafka topics, and additionally define the record key.
 * E.g.: a valid mapping rule would look like this in the ToMaR file:
 * {
 *      "mqttTopic": "sensors/(^[0-9])/type/([^/]+)/data",
 *      "kafkaTopic": "sensors_$1_data",
 *      "kafkaKey": "sensor_$2"
 * }
 * and like this in the MappingRule class:
 * MappingRule(mqttTopicPattern=sensors/(^[0-9])/type/([^/]+)/data, kafkaTopicTemplate=sensors_$1_data, kafkaKey=sensor_$2)
 */
public class MappingRule {
    @JsonProperty("mqttTopic")
    private String mqttTopicPattern;
    @JsonProperty("kafkaTopic")
    private String kafkaTopicTemplate;

    @JsonProperty("kafkaKey")
    private String kafkaKeyTemplate;

    /**
     * Default constructor for MappingRule. Used for deserialization.
     */
    public MappingRule() {
    }

    /**
     * Constructor for MappingRule.
     *
     * @param mqttTopicPattern   the mqtt topic pattern.
     * @param kafkaTopicTemplate the Kafka topic template.
     * @param kafkaKeyTemplate   the Kafka key template.
     */
    public MappingRule(String mqttTopicPattern, String kafkaTopicTemplate, String kafkaKeyTemplate) {
        this.mqttTopicPattern = mqttTopicPattern;
        this.kafkaTopicTemplate = kafkaTopicTemplate;
        this.kafkaKeyTemplate = kafkaKeyTemplate;
    }

    /**
     * Get the Kafka topic template.
     *
     * @return the Kafka topic template.
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
     * Get the record key template.
     *
     * @return the record key template.
     */
    public String getKafkaKeyTemplate() {
        return kafkaKeyTemplate;
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
                ", kafkaKeyTemplate=" + this.kafkaKeyTemplate +
                ")";
    }
}
