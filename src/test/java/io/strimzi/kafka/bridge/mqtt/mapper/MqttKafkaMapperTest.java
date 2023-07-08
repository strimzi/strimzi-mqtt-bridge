/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
        rules.add(new MappingRule("building/{building}/room/{room}", "building_{building}_room_{room}_{noexistingplaceholder}"));

        MqttKafkaMapper mapper = new MqttKafkaMapper(rules);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> mapper.map("fleet/4/vehicle/23"));

        String expectedMessage = "The placeholder {fleet} was not found assigned any value.";
        assertThat("The exception message should be: " + expectedMessage,
                exception.getMessage(), is(expectedMessage));

        Exception otherException = assertThrows(IllegalArgumentException.class, () -> mapper.map("buildings/10/rooms/5/device/3"));

        String otherExpectedMessage = "The placeholder {device} was not found assigned any value.";
        assertThat("The exception message should be: " + otherExpectedMessage,
                otherException.getMessage(), is(otherExpectedMessage));

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
        rules.add(new MappingRule("sensor/#", "sensor_data"));
        rules.add(new MappingRule("sport/tennis/{player}/#", "sports_{player}"));
        rules.add(new MappingRule("+/recipes/#", "my_recipes"));
        rules.add(new MappingRule("{house}/#", "{house}"));

        MqttKafkaMapper mapper = new MqttKafkaMapper(rules);

        // Test fleet/{fleet}/vehicle/{vehicle}/# pattern
        assertThat("Mqtt topic pattern fleet/{fleet}/vehicle/{vehicle}/# should be mapped to fleet_{vehicle}",
                mapper.map("fleet/4/vehicle/23/velocity"), is("fleet_23"));

        // Test building/{building}/room/{room}/# pattern
        assertThat("Mqtt pattern building/{building}/room/{room}/# should be mapped to building_{building}_room_{room}",
                mapper.map("building/4/room/23/temperature"), is("building_4_room_23"));

        // Test building/{building}/# pattern
        assertThat("Mqtt pattern building/{building}/# should be mapped to building_{building}_others",
                mapper.map("building/405/room"), is("building_405_others"));

        assertThat("Mqtt pattern building/# will be mapped to building_101_others because building/{building}/# was defined before building/#",
                mapper.map("building/101"), not("building_others"));

        // Test sensor/# pattern
        assertThat("Mqtt pattern sensor/# should be mapped to sensor_data",
                mapper.map("sensor/temperature"), is("sensor_data"));

        // Test sport/tennis/{player}/# pattern
        assertThat("Mqtt pattern sport/tennis/{player}/# should be mapped to sports_{player}",
                mapper.map("sport/tennis/player1"), is("sports_player1"));

        assertThat("Mqtt pattern sport/tennis/{player}/# should be mapped to sports_{player}",
                mapper.map("sport/tennis/player100/ranking"), is("sports_player100"));

        assertThat("Mqtt pattern sport/tennis/{player}/# should be mapped to sports_{player}",
                mapper.map("sport/tennis/player123/score/wimbledon"), is("sports_player123"));

        // Test +/recipes/# pattern
        assertThat("Mqtt pattern +/recipes/# should be mapped to my_recipes",
                mapper.map("italian/recipes/pizza"), is("my_recipes"));

        assertThat("Mqtt pattern +/recipes/# should be mapped to my_recipes",
                mapper.map("italian/recipes/pasta"), is("my_recipes"));

        assertThat("Mqtt pattern +/recipes/# should be mapped to my_recipes",
                mapper.map("angolan/recipes/calulu/fish"), is("my_recipes"));

        // Test {house}/# pattern
        assertThat("Mqtt pattern {house}/# should be mapped to {house}",
                mapper.map("my_house/temperature"), is("my_house"));

        assertThat("Mqtt pattern {house}/# should be mapped to {house}",
                mapper.map("my_house/temperature/room1"), is("my_house"));
    }
}
