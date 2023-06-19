/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import io.strimzi.kafka.bridge.mqtt.utils.MappingRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for handling all the topic mapping.
 *
 * @see MappingRule
 */
public class MqttKafkaMapper {

    public static final String DEFAULT_KAFKA_TOPIC = "messages_default";
    // matches any character except line terminators. Used to replace the placeholders with {something} in the mqtt pattern.
    private static final String PLACEHOLDER_REGEX = "(.*)";
    // matches any character except a forward slash (/). Used to replace the + in the mqtt pattern.
    private static final String SINGLE_LEVEL_WILDCARD_REGEX = "[^/]+";
    // matches any character after the string. Used to replace the # in the mqtt pattern.
    private static final String MQTT_TOPIC_PLACEHOLDER_REGEX = "\\{\\w+\\}";
    private static final String MQTT_TOPIC_MULTI_LEVEL_WILDCARD_CHARACTER = "#";
    private static final String MQTT_TOPIC_SINGLE_LEVEL_WILDCARD_CHARACTER = "+";
    private static final String MULTIPLE_LEVEL_WILDCARD_REGEX = ".*";
    private final List<MappingRule> rules;
    private final List<Pattern> patterns = new ArrayList<>();

    /**
     * Constructor
     * 
     * Creates a new instance of MqttKafkaMapper.
     */
    public MqttKafkaMapper(List<MappingRule> rules) {
        this.rules = rules;
        this.rules.sort(Comparator.comparing(MappingRule::getMqttTopicPatternLevels).reversed());
        buildRegex();
    }

    /**
     * Maps an MQTT topic to a Kafka topic. The topic is mapped according to the defined mapping rules.
     *
     * @param mqttTopic represents an MQTT topic.
     * @return a valid Kafka topic.
     * @see MappingRule
     */
    public String map(String mqttTopic) {
        for (MappingRule rule : this.rules) {
            Matcher matcher = this.patterns.get(this.rules.indexOf(rule)).matcher(mqttTopic);
            if (matcher.matches()) {
                String mappedKafkaTopic = rule.getKafkaTopicTemplate();
                String[] mqttTopicPatternParts = rule.getMqttTopicPattern().split("/");
                for (String placeholderKey : mqttTopicPatternParts) {
                    if (placeholderKey.matches(MQTT_TOPIC_PLACEHOLDER_REGEX)) {
                        int index = Arrays.asList(mqttTopicPatternParts).indexOf(placeholderKey);
                        String placeholder = mqttTopic.split("/")[index];
                        mappedKafkaTopic = mappedKafkaTopic.replace(placeholderKey, placeholder);
                    }
                }
                return mappedKafkaTopic;
            }
        }
        return DEFAULT_KAFKA_TOPIC;
    }

    /**
     * Helper method for Building the regex expressions for the mapping rules.
     */
    private void buildRegex(){

        //convert the mqtt patterns to a valid regex expression.
        // the mqtt pattern can contain placeholders like {something}, + and #.
        // if the mqtt topic contains a +, we replace it with @singleLevelWildcardRegex
        // if the mqtt topic contains a #, we replace it with @multiLevelWildcardRegex
        // if the mqtt topic contains a placeholder (pattern \{\w+\}), we replace it with @placeholderRegex
        for (MappingRule rule : this.rules) {
            String regex = rule.getMqttTopicPattern().replaceAll(MQTT_TOPIC_PLACEHOLDER_REGEX, PLACEHOLDER_REGEX)
                    .replace(MQTT_TOPIC_MULTI_LEVEL_WILDCARD_CHARACTER, MULTIPLE_LEVEL_WILDCARD_REGEX).replace(MQTT_TOPIC_SINGLE_LEVEL_WILDCARD_CHARACTER, SINGLE_LEVEL_WILDCARD_REGEX);
            patterns.add(Pattern.compile(regex));
        }
    }
}
