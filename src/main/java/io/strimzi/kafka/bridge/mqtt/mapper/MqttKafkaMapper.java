/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Interface for the MqttKafkaMapper. The MqttKafkaMapper is responsible for mapping MQTT topics to Kafka topics.
 */
public abstract class MqttKafkaMapper {

    /** MQTT topic separator */
    public static final String MQTT_TOPIC_SEPARATOR = "/";
    /** List of mapping rules */
    protected final List<MappingRule> rules;
    /** Compiled patterns for the mapping rules */
    protected final List<Pattern> patterns = new ArrayList<>();
    /** Pattern used to find placeholders */
    protected final Pattern placeholderPattern;
    /** Default Kafka topic used when no mapping rule matches */
    protected final String bridgeDefaultTopic;

    /**
     * Constructor
     *
     * @param rules the list of mapping rules.
     * @param placeholderPattern the pattern used to find placeholders.
     * @param bridgeDefaultTopic the default Kafka topic used when no mapping rule matches
     * @see MappingRule
     */
    protected MqttKafkaMapper(List<MappingRule> rules, Pattern placeholderPattern, String bridgeDefaultTopic) {
        this.rules = rules;
        this.placeholderPattern = placeholderPattern;
        this.bridgeDefaultTopic = bridgeDefaultTopic;
    }

    /**
     * Maps an MQTT topic to a Kafka topic. The topic is mapped according to the defined mapping rules.
     *
     * @param mqttTopic MQTT topic to map to a Kafka topic
     * @return a MappingResult object containing the mapped Kafka topic and Kafka key.
     */
    public abstract MappingResult map(String mqttTopic);
}
