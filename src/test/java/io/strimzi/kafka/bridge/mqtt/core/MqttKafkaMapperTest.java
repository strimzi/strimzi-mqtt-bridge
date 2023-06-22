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
import static org.junit.Assert.assertThrows;

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

        rules.add(new MappingRule("sensors/+/data", "sensor_data"));
        rules.add(new MappingRule("devices/{device}/data", "devices_{device}_data"));
        rules.add(new MappingRule("fleet/{fleet}/vehicle/{vehicle}", "fleet_{fleet}"));
        rules.add(new MappingRule("building/{building}/floor/{floor}", "building.{building}.floor.{floor}"));
        rules.add(new MappingRule("term/{number}", "term{number}"));

        MqttKafkaMapper mapper = new MqttKafkaMapper(rules);

        assertThat("Mqtt pattern sensors/+/data should be mapped to sensor_data",
                mapper.map("sensors/4/data"), is("sensor_data"));

        assertThat("Mqtt pattern devices/{device}/data should be mapped to devices_{device}_data",
                mapper.map("devices/4/data"), is("devices_4_data"));

        assertThat("Mqtt pattern fleet/{fleet}/vehicle/{vehicle} should be mapped to fleet_{fleet}",
                mapper.map("fleet/4/vehicle/23"), is("fleet_4"));

        assertThat("building/{building}/floor/{floor} should be mapped to building.{building}.floor.{floor}",
                mapper.map("building/4/floor/23"), is("building.4.floor.23"));

        assertThat("Mqtt pattern term/{number} should be mapped to term{number}",
                mapper.map("term/4"), is("term4"));
    }


    /**
     * Test the mapping of single level topics.
     */
    @Test
    public void testIllegalPlaceholder() {

        List<MappingRule> rules = new ArrayList<>();
        rules.add(new MappingRule("fleet/{flee}/vehicle/{vehicle}", "fleet_{fleet}"));
        rules.add(new MappingRule("buildings/+/rooms/+/device/+", "buildings_{building}_rooms_{room}_device_{device}"));

        MqttKafkaMapper mapper = new MqttKafkaMapper(rules);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> mapper.map("fleet/4/vehicle/23"));

        String expectedMessage = "One or more placeholders were not assigned any value.";
        assertThat("The exception message should be: " + expectedMessage,
                exception.getMessage(), is(expectedMessage));

        Exception otherException = assertThrows(IllegalArgumentException.class, () -> mapper.map("buildings/10/rooms/5/device/3"));

        assertThat("The exception message should be: " + expectedMessage,
                otherException.getMessage(), is(expectedMessage));

    }

    /**
     * Test the mapping of multi level topics.
     */
    @Test
    public void testMultiLevel() {
        List<MappingRule> rules = new ArrayList<>();

        rules.add(new MappingRule("building/{building}/room/{room}/#", "building_{building}_room_{room}"));
        rules.add(new MappingRule("building/{building}/#", "building_{building}_others"));
        rules.add(new MappingRule("building/#", "building_others"));
        rules.add(new MappingRule("fleet/{fleet}/vehicle/{vehicle}/#", "fleet_{vehicle}"));

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
