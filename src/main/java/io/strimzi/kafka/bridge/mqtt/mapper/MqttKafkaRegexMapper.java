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
     * Creates a new instance of MqttKafkaRegexMapper.
     */
    public MqttKafkaRegexMapper(List<MappingRule> rules) {
        super(rules, Pattern.compile(MQTT_TOPIC_DOLLAR_PLACEHOLDER_REGEX));
    }

    @Override
    public MappingResult map(String mqttTopic) {
        for (MappingRule rule : this.rules) {
            Matcher matcher = this.patterns.get(this.rules.indexOf(rule)).matcher(mqttTopic);
            if (matcher.matches()) {
                String mappedKafkaTopic = rule.getKafkaTopicTemplate();
                String kafkaKey = rule.getKafkaKeyTemplate();

                for (int i = 1; i < matcher.groupCount() + 1; i++) {
                    mappedKafkaTopic = mappedKafkaTopic.replace("$" + i, matcher.group(i));
                    kafkaKey = kafkaKey != null ? kafkaKey.replace("$" + i, matcher.group(i)) : null;
                }

                // check for pending placeholders replacement in the Kafka topic
                checkPlaceholder(mappedKafkaTopic);

                if (kafkaKey != null) {
                    // check for pending placeholders replacement in the Kafka key.
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
        this.rules.forEach(rule-> this.patterns.add(Pattern.compile(rule.getMqttTopicPattern())));
    }

    /**
     * Checks if there are any pending placeholders in the Kafka topic or Kafka key template.
     *
     * @param template the placeholder to check.
     */
    private void checkPlaceholder(String template) {
        Matcher matcher = this.placeholderPattern.matcher(template);
        if (matcher.find()) {
            throw new IllegalArgumentException("The placeholder " + matcher.group() + " was not found or assigned any value.");
        }
    }
}
