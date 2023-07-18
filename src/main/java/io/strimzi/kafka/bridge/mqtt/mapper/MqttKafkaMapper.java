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

    // default Kafka topic. Used when no mapping rule matches the mqtt topic.
    public static final String DEFAULT_KAFKA_TOPIC = "messages_default";

    protected final List<MappingRule> rules;
    protected final List<Pattern> patterns = new ArrayList<>();
    protected final Pattern placeholderPattern;

    /**
     * Constructor
     *
     * @param rules the list of mapping rules.
     * @param placeholderPattern the pattern used to find placeholders.
     * @see MappingRule
     */
    protected MqttKafkaMapper(List<MappingRule> rules, Pattern placeholderPattern) {
        this.rules = rules;
        this.placeholderPattern = placeholderPattern;
        this.buildOrCompilePatterns();
    }

    /**
     * Maps an MQTT topic to a Kafka topic. The topic is mapped according to the defined mapping rules.
     *
     * @param mqttTopic
     * @return a MappingResult object containing the mapped Kafka topic and Kafka key.
     */
    public abstract MappingResult map(String mqttTopic);

    /**
     * Helper method for Building the regex expressions for the mapping rules.
     */
    protected abstract void buildOrCompilePatterns();
}
