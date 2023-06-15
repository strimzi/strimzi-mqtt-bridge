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

import java.io.BufferedReader;
import java.io.FileReader;
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


    /**
     * Read the mapping rules from the mapping_rules.json file and create a MqttKafkaMapper object.
     * Check @src/test/resources/mapping_rules.json to add or modify the mapping rules.
     *
     * @throws IOException
     */
    @Before
    public void setUp() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/mapping_rules.json"));
        ObjectMapper objectMapper = new ObjectMapper();
        StringBuilder jsonBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonBuilder.append(line);
        }
        mappingRules = objectMapper.readValue(jsonBuilder.toString(), new TypeReference<>() {
        });
        mappingRules.sort(Comparator.comparing(MappingRule::getMqttTopicPatternLevels).reversed());
        mqttKafkaMapper = new MqttKafkaMapper(mappingRules);
    }

    /**
     * Test the mapping of single level topics.
     */
    @Test()
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
                map("sensor/temperature"), is("messages_default"));

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
     * Test rules loading from json file.
     */
    @Test
    public void testLoadMappingRules() {
        assertThat("Should load 7 mapping rules",
                mappingRules.size(), is(7));

    }

    /**
     * Helper method to map an MQTT topic to a Kafka topic.
     */
    protected String map(String mqttTopic) {
        return mqttKafkaMapper.map(mqttTopic);
    }
}
