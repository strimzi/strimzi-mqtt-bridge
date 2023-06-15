/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import io.strimzi.kafka.bridge.mqtt.utils.MappingRule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Responsible for handling all the topic mapping.
 *
 * @see MappingRule
 */
public class MqttKafkaMapper {

    private static String DEFAULT_KAFKA_TOPIC = "messages_default";
    private ArrayList<MappingRule> rules;

    /**
     * Constructor
     *
     * Creates a new instance of MqttKafkaMapper.
     */
    public MqttKafkaMapper(){
        rules = new ArrayList<>();
        rules.add(new MappingRule("building_{building}_room_{room}", "building/{building}/room/{room}/#"));
        rules.add(new MappingRule("sensor_data", "sensors/+/data"));
        rules.add(new MappingRule("devices_{device}_data", "devices/{device}/data"));
        rules.add(new MappingRule("fleet_{vehicle}", "fleet/{fleet}/vehicle/{vehicle}/#"));
        rules.add(new MappingRule("building_{building}_others","building/{building}/#"));
        rules.add(new MappingRule("sensor_others", "sensors/#"));
        rules.add(new MappingRule("building_others", "building/#"));
        rules.sort(Comparator.comparing(MappingRule::getMqttTopicPatternLevels).reversed());
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
            //convert the mqtt pattern to a valid regex expression.
            String regex = rule.getMqttTopicPattern().replaceAll("\\{\\w+\\}", "(.*)")
                    .replace("#", ".*").replace("+", "[^/]+");
            if (mqttTopic.matches(regex)) {
                String mappedKafkaTopic = rule.getKafkaTopicTemplate();
                String[] mqttTopicPatternParts = rule.getMqttTopicPattern().split("/");
                for (String placeholderKey : mqttTopicPatternParts) {
                    if (placeholderKey.matches("\\{\\w+\\}")) {
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
}
