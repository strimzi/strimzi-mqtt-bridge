/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for handling all the topic mapping rules defined with regular expressions.
 */
public class MqttKafkaRegexMapper extends MqttKafkaMapper {

    // used to find any expression starting with a $ followed by upto 2 digits number. E.g. $1, this is known as a placeholder.
    public static final String MQTT_TOPIC_DOLLAR_PLACEHOLDER_REGEX = "\\$(\\d{1,2})";

    /**
     * Constructor
     * Creates a new instance of MqttKafkaSimpleMapper.
     */
    public MqttKafkaRegexMapper(List<MappingRule> rules) {
        super(rules, Pattern.compile(MQTT_TOPIC_DOLLAR_PLACEHOLDER_REGEX));
    }

    /**
     * Maps an MQTT topic to a Kafka topic. The topic is mapped according to the defined mapping rules.
     *
     * @param mqttTopic represents an MQTT topic.
     * @return a MappingResult object containing the mapped kafka topic and kafka key.
     * @see MappingRule
     * @see MappingResult
     */
    @Override
    public MappingResult map(String mqttTopic) {
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

                if (kafkaKey != null) {
                    // check for pending placeholders replacement in the kafka key.
                    checkPlaceholder(kafkaKey);
                }

                // return the first match
                return new MappingResult(mappedKafkaTopic, kafkaKey);
            }
        }
        return new MappingResult(MqttKafkaMapper.DEFAULT_KAFKA_TOPIC, null);
    }

    @Override
    protected void buildOrCompilePatterns() {
        this.rules.forEach(e -> patterns.add(Pattern.compile(e.getMqttTopicPattern())));
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
