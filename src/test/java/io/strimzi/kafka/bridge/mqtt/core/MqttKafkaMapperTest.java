/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strimzi.kafka.bridge.mqtt.utils.MappingRule;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link MqttKafkaMapper}
 */
public class MqttKafkaMapperTest {

    private ArrayList<MappingRule> mappingRules;
    private MqttKafkaMapper mqttKafkaMapper;
    //define the mapping rules as a JSON string. Add more rules if needed.
    private static final String TOPIC_MAPPING_RULES_JSON =
            "[" +
                    "{\"kafkaTopic\":\"building_{building}_room_{room}\",\"mqttTopic\":\"building/{building}/room/{room}/#\"}," +
                    "{\"kafkaTopic\":\"sensor_data\",\"mqttTopic\":\"sensors/+/data\"}," +
                    "{\"kafkaTopic\":\"devices_{device}_data\",\"mqttTopic\":\"devices/{device}/data\"}," +
                    "{\"kafkaTopic\":\"fleet_{vehicle}\",\"mqttTopic\":\"fleet/{fleet}/vehicle/{vehicle}/#\"}," +
                    "{\"kafkaTopic\":\"building_{building}_others\",\"mqttTopic\":\"building/{building}/#\"}," +
                    "{\"kafkaTopic\":\"sensor_others\",\"mqttTopic\":\"sensors/#\"}," +
                    "{\"kafkaTopic\":\"building_others\",\"mqttTopic\":\"building/#\"}" + "]";

    /**
     * Load all the mapping rules before running the tests.
     */
    @Before
    public void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mappingRules = mapper.readValue(TOPIC_MAPPING_RULES_JSON, new TypeReference<>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        mappingRules.sort(Comparator.comparing(MappingRule::getMqttTopicPatternLevels).reversed());
        mqttKafkaMapper = new MqttKafkaMapper(mappingRules);
    }

    /**
     * Test the mapping of single level topics.
     */
    @Test
    public void testSingleLevel() {
        assertThat("Mqtt pattern sensors/+/data should be mapped to sensor_data",
                map("sensors/4/data"), is("sensor_data"));

        assertThat("Mqtt pattern sensors/# should be mapped to sensor_others",
                map("sensors/25/temperature/data"), is("sensor_others"));

        assertThat("Mqtt pattern sensors/# should be mapped to sensor_others",
                map("sensors/25/data/humidity"), is("sensor_others"));

        assertThat("Mqtt pattern devices/{device}/data should be mapped to devices_{device}_data",
                map("devices/4/data"), is("devices_4_data"));

        assertThat("Should use the default topic when no mapping rule is found",
                map("sensor/temperature"), is(DEFAULT_KAFKA_TOPIC));

        assertThat("Should use the default topic when no mapping rule is found",
                map("devices/1"), is("messages_default"));

    }

    /**
     * Test the mapping of multi level topics.
     */
    @Test
    public void testMultiLevel() {
        assertThat("Mqtt topic pattern fleet/{fleet}/vehicle/{vehicle}/# should be mapped to fleet_{vehicle}",
                map("fleet/4/vehicle/23/velocity"), is("fleet_23"));

        assertThat("Mqtt pattern building/{building}/room/{room}/# should be mapped to building_{building}_room_{room}",
                map("building/4/room/23/temperature"), is("building_4_room_23"));

        assertThat("Mqtt pattern building/{building}/# should be mapped to building_{building}_others",
                map("building/405/room"), is("building_405_others"));

        assertThat("Mqtt pattern building/# should be mapped to building_others",
                map("building/101"), is("building_others"));
    }

    /**
     * Helper method to map an MQTT topic to a Kafka topic.
     */
    protected String map(String mqttTopic) {
        return mqttKafkaMapper.map(mqttTopic);
    }
}
