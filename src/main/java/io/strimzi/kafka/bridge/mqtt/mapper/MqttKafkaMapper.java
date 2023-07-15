/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

import java.util.List;
import java.util.ArrayList;
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

    // used to find any expression starting with a $ followed by upto 2 digits number. E.g. $1, this is known as a placeholder.
    private static final String MQTT_TOPIC_PLACEHOLDER_REGEX = "\\$(\\d{1,2})";

    private final List<MappingRule> rules;
    private final List<Pattern> patterns = new ArrayList<>();
    private final Pattern placeholderPattern;

    /**
     * Constructor
     * Creates a new instance of MqttKafkaMapper.
     */
    public MqttKafkaMapper(List<MappingRule> rules) {
        this.rules = rules;
        this.placeholderPattern = Pattern.compile(MQTT_TOPIC_PLACEHOLDER_REGEX);

        // compile the patterns for each rule
        this.rules.forEach(e -> this.patterns.add(Pattern.compile(e.getMqttTopicPattern())));
    }

    /**
     * Maps an MQTT topic to a Kafka topic. The topic is mapped according to the defined mapping rules.
     *
     * @param mqttTopic represents an MQTT topic.
     * @return a valid Kafka topic.
     * @see MappingRule
     */
    public MappingResult map(String mqttTopic) {
        MappingResult mappingResult = new MappingResult(DEFAULT_KAFKA_TOPIC, null);

        for (MappingRule rule : this.rules) {
            Matcher matcher = this.patterns.get(this.rules.indexOf(rule)).matcher(mqttTopic);
            if (matcher.matches()) {
                String mappedKafkaTopic = rule.getKafkaTopicTemplate();
                String kafkaKey = rule.getKafkaKey();

                for (int i = 1; i < matcher.groupCount() + 1; i++) {
                    mappedKafkaTopic = mappedKafkaTopic.replace("$" + i, matcher.group(i));
                    kafkaKey = kafkaKey != null ? kafkaKey.replace("$" + i, matcher.group(i)) : null;
                }

                // check for pending placeholders replacement in the kafka topic
                checkPlaceholder(mappedKafkaTopic);
                // set the mapped kafka topic to the result
                mappingResult.setKafkaTopic(mappedKafkaTopic);

                if (kafkaKey != null) {
                    // check for pending placeholders replacement in the kafka key.
                    checkPlaceholder(kafkaKey);
                    // set the mapped kafka key to the result
                    mappingResult.setKafkaKey(kafkaKey);
                }

                // return the first match
                return mappingResult;
            }
        }
        return mappingResult;
    }

    /**
     * Checks if there are any pending placeholders in the kafka topic or kafka key.
     *
     * @param placeholder the placeholder to check.
     */
    private void checkPlaceholder(String placeholder) {
        Matcher matcher = this.placeholderPattern.matcher(placeholder);
        if (matcher.find()) {
            throw new IllegalArgumentException("The placeholder " + matcher.group() + " was not found or assigned any value.");
        }
    }
}
