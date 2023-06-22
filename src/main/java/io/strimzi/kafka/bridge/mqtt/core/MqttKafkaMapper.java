/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import io.strimzi.kafka.bridge.mqtt.utils.MappingRule;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for handling all the topic mapping.
 *
 * @see MappingRule
 */
public class MqttKafkaMapper {

    // default kafka topic. Used when no mapping rule matches the mqtt topic.
    public static final String DEFAULT_KAFKA_TOPIC = "messages_default";

    // find any word inside a curly bracket. E.g. {something}, this is known as a placeholder.
    private static final String MQTT_TOPIC_PLACEHOLDER_REGEX = "\\{\\w+\\}";

    // identifies a single level wildcard character in the mqtt pattern. E.g. sensors/+/data
    private static final String MQTT_TOPIC_SINGLE_LEVEL_WILDCARD_CHARACTER = "+";

    // Regex expression used to replace the + in the mqtt pattern.
    private static final String SINGLE_LEVEL_WILDCARD_REGEX = "[^/]+";

    // identifies a multi level wildcard character in the mqtt pattern. E.g. sensors/#
    private static final String MQTT_TOPIC_MULTI_LEVEL_WILDCARD_CHARACTER = "#";

    // matches any character after the string. Used to replace the # in the mqtt pattern.
    private static final String MULTIPLE_LEVEL_WILDCARD_REGEX = ".*";
    private final List<MappingRule> rules;
    private final List<Pattern> patterns = new ArrayList<>();
    private final Pattern placeholderPattern;

    /**
     * Constructor
     * 
     * Creates a new instance of MqttKafkaMapper.
     */
    public MqttKafkaMapper(List<MappingRule> rules) {
        this.rules = rules;
        this.placeholderPattern = Pattern.compile(MQTT_TOPIC_PLACEHOLDER_REGEX);
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
                HashMap<String, String> placeholders = new HashMap<>();

                String mappedKafkaTopic = rule.getKafkaTopicTemplate();

                // find MQTT_TOPIC_PLACEHOLDER_REGEX in the kafkaTopicTemplate.
                Matcher placeholderMatcher = this.placeholderPattern.matcher(rule.getKafkaTopicTemplate());
                while (placeholderMatcher.find()) {
                    String placeholderKey = placeholderMatcher.group();
                    placeholders.put(placeholderKey, null);
                }

                if (!placeholders.isEmpty()) {
                    Matcher mqttTopicMatcher = this.placeholderPattern.matcher(rule.getMqttTopicPattern());

                    // find the placeholders in the mqtt topic pattern and assign them a value.
                    while (mqttTopicMatcher.find()) {
                        String placeholderKey = mqttTopicMatcher.group();
                        String placeholderValue = matcher.group(removeBrackets(placeholderKey));
                        placeholders.put(placeholderKey, placeholderValue);
                    }

                    //build the kafka topic using the placeholders.
                    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                        if (entry.getValue() != null) {
                            mappedKafkaTopic = mappedKafkaTopic.replace(entry.getKey(), entry.getValue());
                        } else {
                            throw new IllegalArgumentException("The placeholder " + entry.getKey() + " was not found in the mqtt topic.");
                        }
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
    private void buildRegex() {

        //convert the mqtt patterns to a valid regex expression.
        // the mqtt pattern can contain placeholders like {something}, + and #.
        // if the mqtt topic contains a +, we replace it with @singleLevelWildcardRegex
        // if the mqtt topic contains a #, we replace it with @multiLevelWildcardRegex
        // if the mqtt topic contains a placeholder (pattern \{\w+\}), we replace it with @placeholderRegex
        String[] mqttTopicPatternParts;
        StringBuilder ruleRegex;
        for (MappingRule rule : this.rules) {
            mqttTopicPatternParts = rule.getMqttTopicPattern().split("/");
            ruleRegex = new StringBuilder();
            for (String part : mqttTopicPatternParts) {
                if (part.matches(MQTT_TOPIC_PLACEHOLDER_REGEX)) {
                    ruleRegex.append(buildNamedRegexExpression(part));
                } else if (part.equals(MQTT_TOPIC_SINGLE_LEVEL_WILDCARD_CHARACTER)) {
                    ruleRegex.append(SINGLE_LEVEL_WILDCARD_REGEX);
                } else if (part.equals(MQTT_TOPIC_MULTI_LEVEL_WILDCARD_CHARACTER)) {
                    ruleRegex.append(MULTIPLE_LEVEL_WILDCARD_REGEX);
                } else {
                    ruleRegex.append(part);
                }
                ruleRegex.append("/");
            }
            // remove the last slash
            ruleRegex.deleteCharAt(ruleRegex.length() - 1);
            // compile the regex expression for the rule.
            patterns.add(Pattern.compile(ruleRegex.toString()));
        }
    }

    /**
     * Helper method for building a named regex expression.
     *
     * A named regex expression is a regex expression that contains a named capturing group.
     *
     * E.g. (?<groupName>regexExpression)
     *
     * @param placeholder represents a placeholder in the mqtt pattern.
     * @return a named regex expression.
     */
    private String buildNamedRegexExpression(String placeholder) {
        String groupName = removeBrackets(placeholder);
        return "(?<" + groupName + ">[^/]+)";
    }

    /**
     * Helper method for removing the curly brackets from a placeholder.
     * @param placeholder
     * @return a placeholder without the curly brackets.
     */
    private String removeBrackets(String placeholder) {
        return placeholder.replaceAll("\\{+|\\}+", "");
    }
}
