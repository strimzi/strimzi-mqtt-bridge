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
import java.util.Comparator;
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

    // Regex used to replace the placeholders with {something} in the mqtt pattern.
    private static final String PLACEHOLDER_REGEX = "(.*)";

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

    /**
     * Constructor
     * <p>
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
            HashMap<String, String> placeholders = new HashMap<>();

            if (matcher.matches()) {
                String mappedKafkaTopic = rule.getKafkaTopicTemplate();
                String[] mqttTopicPatternParts = rule.getMqttTopicPattern().split("/");

                // find MQTT_TOPIC_PLACEHOLDER_REGEX in the kafkaTopicTemplate.
                Matcher placeholderMatcher = patterns.get(patterns.size() - 1).matcher(rule.getKafkaTopicTemplate());
                while (placeholderMatcher.find()) {
                    placeholders.put(placeholderMatcher.group(), null);
                }

                for (String placeholderKey : mqttTopicPatternParts) {
                    if (placeholderKey.matches(MQTT_TOPIC_PLACEHOLDER_REGEX) && placeholders.containsKey(placeholderKey)) {
                        placeholders.put(placeholderKey, matcher.group(removeBrackets(placeholderKey)));
                    }
                }

                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    if (entry.getValue() != null) {
                        mappedKafkaTopic = mappedKafkaTopic.replace(entry.getKey(), entry.getValue());
                    } else {
                        throw new IllegalArgumentException("The placeholder " + entry.getKey() + " was not assigned any value.");
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
            int lastElementIndex = mqttTopicPatternParts.length - 1;
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
                if (!part.equals(mqttTopicPatternParts[lastElementIndex])) {
                    ruleRegex.append("/");
                }
            }
            patterns.add(Pattern.compile(ruleRegex.toString()));
        }

        // add the regex for the placeholders in the end of the patterns list.
        patterns.add(Pattern.compile(MQTT_TOPIC_PLACEHOLDER_REGEX));
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
