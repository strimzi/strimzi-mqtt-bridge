/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import io.strimzi.kafka.bridge.mqtt.utils.MappingRule;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link MqttKafkaMapper}
 */
public class MqttKafkaMapperTest {


    /**
     * Test for default topic.
     * If the MQTT topic does not match any of the mapping rules, the default topic is used.
     * E.g. if the Topic Mapping Rules is empty, the default topic is used by default.
     */
    @Test
    public void testDefaultTopic() {
        List<MappingRule> rules = new ArrayList<>();

        MqttKafkaMapper mapper = new MqttKafkaMapper(rules);

        assertThat("Should use the default topic when no mapping pattern matches.",
                mapper.map("sensor/temperature"), is(MqttKafkaMapper.DEFAULT_KAFKA_TOPIC));
    }

    /**
     * Test the mapping of single level topics.
     */
    @Test
    public void testSingleLevel() {
        List<MappingRule> rules = new ArrayList<>();

        rules.add(new MappingRule("sensor_data", "sensors/+/data"));
        rules.add(new MappingRule("devices_{device}_data", "devices/{device}/data"));
        rules.add(new MappingRule("fleet_{fleet}", "fleet/{fleet}/vehicle/{vehicle}"));

        MqttKafkaMapper mapper = new MqttKafkaMapper(rules);

        assertThat("Mqtt pattern sensors/+/data should be mapped to sensor_data",
                mapper.map("sensors/4/data"), is("sensor_data"));

        assertThat("Mqtt pattern devices/{device}/data should be mapped to devices_{device}_data",
                mapper.map("devices/4/data"), is("devices_4_data"));

        assertThat("Mqtt pattern fleet/{fleet}/vehicle/{vehicle} should be mapped to fleet_{fleet}",
                mapper.map("fleet/4/vehicle/23"), is("fleet_4"));
    }

    /**
     * Test the mapping of multi level topics.
     */
    @Test
    public void testMultiLevel() {
        List<MappingRule> rules = new ArrayList<>();

        rules.add(new MappingRule("building_{building}_room_{room}", "building/{building}/room/{room}/#"));
        rules.add(new MappingRule("building_{building}_others", "building/{building}/#"));
        rules.add(new MappingRule("building_others", "building/#"));
        rules.add(new MappingRule("fleet_{vehicle}", "fleet/{fleet}/vehicle/{vehicle}/#"));

        MqttKafkaMapper mapper = new MqttKafkaMapper(rules);

        assertThat("Mqtt topic pattern fleet/{fleet}/vehicle/{vehicle}/# should be mapped to fleet_{vehicle}",
                mapper.map("fleet/4/vehicle/23/velocity"), is("fleet_23"));

        assertThat("Mqtt pattern building/{building}/room/{room}/# should be mapped to building_{building}_room_{room}",
                mapper.map("building/4/room/23/temperature"), is("building_4_room_23"));

        assertThat("Mqtt pattern building/{building}/# should be mapped to building_{building}_others",
                mapper.map("building/405/room"), is("building_405_others"));

        assertThat("Mqtt pattern building/# should be mapped to building_others",
                mapper.map("building/101"), is("building_others"));
    }
}
