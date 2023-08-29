/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for handling all the topic mapping using named placeholders instead of regular expressions.
 *
 * @see MappingRule
 * @see MqttKafkaMapper
 * @see MqttKafkaRegexMapper
 */
public class MqttKafkaSimpleMapper extends MqttKafkaMapper {

    // find any word inside a curly bracket. E.g. {something}, this is known as a placeholder.
    private static final String MQTT_TOPIC_PLACEHOLDER_REGEX = "\\{\\w+\\}";

    // identifies a single level wildcard character in the mqtt pattern. E.g. sensors/+/data
    private static final String MQTT_TOPIC_SINGLE_LEVEL_WILDCARD_CHARACTER = "+";

    // Regex expression used to replace the + in the mqtt pattern.
    private static final String SINGLE_LEVEL_WILDCARD_REGEX = "[^/]+";

    // identifies a multi level wildcard character in the mqtt pattern. E.g. sensors/#
    private static final String MQTT_TOPIC_MULTI_LEVEL_WILDCARD_CHARACTER = "#";

    // used to replace the # in the mqtt pattern.
    public static final String WILDCARD_REGEX = "(?:\\/.*)?$";

    /**
     * Constructor.
     *
     * @param rules the list of mapping rules.
     */
    public MqttKafkaSimpleMapper(List<MappingRule> rules) {
        super(rules, Pattern.compile(MQTT_TOPIC_PLACEHOLDER_REGEX));
        this.buildOrCompilePatterns();
    }

    @Override
    public MappingResult map(String mqttTopic) {
        for (MappingRule rule : this.rules) {
            Matcher matcher = this.patterns.get(this.rules.indexOf(rule)).matcher(mqttTopic);

            if (matcher.matches()) {
                HashMap<String, String> placeholders = new HashMap<>();

                String mappedKafkaTopic = rule.getKafkaTopicTemplate();
                String kafkaKey = rule.getKafkaKeyTemplate();

                // find MQTT_TOPIC_PLACEHOLDER_REGEX in the kafkaTopicTemplate.
                Matcher placeholderMatcher = this.placeholderPattern.matcher(rule.getKafkaTopicTemplate());
                while (placeholderMatcher.find()) {
                    String placeholderKey = placeholderMatcher.group();
                    placeholders.put(placeholderKey, null);
                }

                // find MQTT_TOPIC_PLACEHOLDER_REGEX in the kafkaKey
                if (kafkaKey != null) {
                    placeholderMatcher = this.placeholderPattern.matcher(kafkaKey);
                    while (placeholderMatcher.find()) {
                        String placeholderKey = placeholderMatcher.group();
                        placeholders.put(placeholderKey, null);
                    }
                }

                if (!placeholders.isEmpty()) {
                    Matcher mqttTopicMatcher = this.placeholderPattern.matcher(rule.getMqttTopicPattern());

                    // find the placeholders in the mqtt topic pattern and assign them a value.
                    while (mqttTopicMatcher.find()) {
                        String placeholderKey = mqttTopicMatcher.group();
                        String placeholderValue = matcher.group(removeBrackets(placeholderKey));
                        placeholders.put(placeholderKey, placeholderValue);
                    }

                    // build the Kafka topic using the placeholders.
                    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                        if (entry.getValue() != null) {
                            mappedKafkaTopic = mappedKafkaTopic.replace(entry.getKey(), entry.getValue());
                            kafkaKey = kafkaKey != null ? kafkaKey.replace(entry.getKey(), entry.getValue()) : null;
                        } else {
                            throw new IllegalArgumentException("The placeholder " + entry.getKey() + " was not found assigned any value.");
                        }
                    }
                }
                return new MappingResult(mappedKafkaTopic, kafkaKey);
            }
        }
        return new MappingResult(MqttKafkaMapper.DEFAULT_KAFKA_TOPIC, null);
    }

    /**
     * Helper method for Building the regex expressions for the mapping rules.
     */
    private void buildOrCompilePatterns() {

        // convert the mqtt patterns to a valid regex expression.
        // the mqtt pattern can contain placeholders like {something}, + and #.
        // if the mqtt topic contains a +, we replace it with @singleLevelWildcardRegex
        // if the mqtt topic contains a #, we replace it with @multiLevelWildcardRegex
        // if the mqtt topic contains a placeholder (pattern \{\w+\}), we replace it with @placeholderRegex
        String[] mqttTopicPatternParts;
        StringBuilder ruleRegex;
        for (MappingRule rule : this.rules) {
            mqttTopicPatternParts = rule.getMqttTopicPattern().split(MQTT_TOPIC_SEPARATOR);
            ruleRegex = new StringBuilder();
            for (String part : mqttTopicPatternParts) {
                if (part.matches(MQTT_TOPIC_PLACEHOLDER_REGEX)) {
                    ruleRegex.append(buildNamedRegexExpression(part));
                } else if (part.equals(MQTT_TOPIC_SINGLE_LEVEL_WILDCARD_CHARACTER)) {
                    ruleRegex.append(SINGLE_LEVEL_WILDCARD_REGEX);
                } else if (part.equals(MQTT_TOPIC_MULTI_LEVEL_WILDCARD_CHARACTER)) {
                    if (ruleRegex.length() > 1) {
                        ruleRegex.deleteCharAt(ruleRegex.length() - 1);
                    }
                    ruleRegex.append(WILDCARD_REGEX);
                } else {
                    ruleRegex.append(part);
                }
                ruleRegex.append(MQTT_TOPIC_SEPARATOR);
            }
            // remove the last slash
            ruleRegex.deleteCharAt(ruleRegex.length() - 1);
            // compile the regex expression for the rule.
            patterns.add(Pattern.compile(ruleRegex.toString()));
        }
    }

    /**
     * Helper method for building a named regex expression.
     * A named regex expression is a regex expression that contains a named capturing group.
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
     *
     * @param placeholder represents a placeholder in the pattern.
     * @return a placeholder without the curly brackets.
     */
    private String removeBrackets(String placeholder) {
        return placeholder.replaceAll("\\{+|\\}+", "");
    }
}
