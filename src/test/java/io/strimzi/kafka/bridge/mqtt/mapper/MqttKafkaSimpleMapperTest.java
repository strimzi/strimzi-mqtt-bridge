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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for @{@link MqttKafkaSimpleMapper}.
 *
 * @see MqttKafkaSimpleMapper
 */
public class MqttKafkaSimpleMapperTest {

    /**
     * Test for default topic.
     * If the MQTT topic does not match any of the mapping rules, the default topic is used.
     * E.g. if the Topic Mapping Rules is empty, the default topic is used by default.
     */
    @Test
    public void testDefaultTopic() {
        List<MappingRule> rules = new ArrayList<>();

        MqttKafkaSimpleMapper mapper = new MqttKafkaSimpleMapper(rules);

        assertThat("Should use the default topic when no mapping pattern matches.",
                mapper.map("sensor/temperature").kafkaTopic(), is(MqttKafkaMapper.DEFAULT_KAFKA_TOPIC));
    }

    /**
     * Test the mapping of single level topics.
     */
    @Test
    public void testSingleLevel() {
        List<MappingRule> rules = new ArrayList<>();

        rules.add(new MappingRule("sensors/+/data", "sensor_data", "sensor"));
        rules.add(new MappingRule("devices/{device}/data", "devices_{device}_data", "device_{device}"));
        rules.add(new MappingRule("fleet/{fleet}/vehicle/{vehicle}", "fleet_{fleet}", "vehicle_{vehicle}"));
        rules.add(new MappingRule("building/{building}/floor/{floor}", "building.{building}.floor.{floor}", "floor_{floor}"));
        rules.add(new MappingRule("term/{number}", "term{number}", null));

        MqttKafkaSimpleMapper mapper = new MqttKafkaSimpleMapper(rules);

        assertThat("Mqtt pattern sensors/+/data should be mapped to sensor_data",
                mapper.map("sensors/4/data").kafkaTopic(), is("sensor_data"));

        assertThat("The key for sensors/+/data should be sensor",
                mapper.map("sensors/4/data").kafkaKey(), is("sensor"));

        assertThat("Mqtt pattern devices/{device}/data should be mapped to devices_{device}_data",
                mapper.map("devices/4/data").kafkaTopic(), is("devices_4_data"));

        assertThat("The key for devices/{device}/data should be device_{device}",
                mapper.map("devices/4/data").kafkaKey(), is("device_4"));

        assertThat("Mqtt pattern fleet/{fleet}/vehicle/{vehicle} should be mapped to fleet_{fleet}",
                mapper.map("fleet/4/vehicle/23").kafkaTopic(), is("fleet_4"));

        assertThat("The key for fleet/{fleet}/vehicle/{vehicle} should be vehicle_{vehicle}",
                mapper.map("fleet/4/vehicle/23").kafkaKey(), is("vehicle_23"));

        assertThat("building/{building}/floor/{floor} should be mapped to building.{building}.floor.{floor}",
                mapper.map("building/4/floor/23").kafkaTopic(), is("building.4.floor.23"));

        assertThat("The key for building/{building}/floor/{floor} should be floor_{floor}",
                mapper.map("building/4/floor/23").kafkaKey(), is("floor_23"));

        assertThat("Mqtt pattern term/{number} should be mapped to term{number}",
                mapper.map("term/4").kafkaTopic(), is("term4"));

        assertThat("The key for term/{number} should be null",
                mapper.map("term/4").kafkaKey(), is(nullValue()));
    }


    /**
     * Test the mapping of single level topics.
     */
    @Test
    public void testIllegalPlaceholder() {

        List<MappingRule> rules = new ArrayList<>();
        rules.add(new MappingRule("fleet/{flee}/vehicle/{vehicle}", "fleet_{fleet}", null));
        rules.add(new MappingRule("buildings/+/rooms/+/device/+", "buildings_{building}_rooms_{room}_device_{device}", "building"));
        rules.add(new MappingRule("building/{building}/room/{room}", "building_{building}_room_{room}_{noexistingplaceholder}", null));

        MqttKafkaSimpleMapper mapper = new MqttKafkaSimpleMapper(rules);

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

        rules.add(new MappingRule("building/{building}/room/{room}/#", "building_{building}_room_{room}", "building"));
        rules.add(new MappingRule("building/{building}/#", "building_{building}_others", "building_{building}"));
        rules.add(new MappingRule("building/#", "building_others", null));
        rules.add(new MappingRule("fleet/{fleet}/vehicle/{vehicle}/#", "fleet_{vehicle}", "vehicle_{vehicle}"));
        rules.add(new MappingRule("sensor/#", "sensor_data", "sensor"));
        rules.add(new MappingRule("sport/tennis/{player}/#", "sports_{player}", "player_{player}"));
        rules.add(new MappingRule("+/recipes/#", "my_recipes", null));
        rules.add(new MappingRule("{house}/#", "{house}", null));

        MqttKafkaSimpleMapper mapper = new MqttKafkaSimpleMapper(rules);

        // Test fleet/{fleet}/vehicle/{vehicle}/# pattern
        assertThat("Mqtt topic pattern fleet/{fleet}/vehicle/{vehicle}/# should be mapped to fleet_{vehicle}",
                mapper.map("fleet/4/vehicle/23/velocity").kafkaTopic(), is("fleet_23"));

        assertThat("The key for fleet/{fleet}/vehicle/{vehicle}/# should be vehicle_{vehicle}",
                mapper.map("fleet/4/vehicle/23/velocity").kafkaKey(), is("vehicle_23"));

        // Test building/{building}/room/{room}/# pattern
        assertThat("Mqtt pattern building/{building}/room/{room}/# should be mapped to building_{building}_room_{room}",
                mapper.map("building/4/room/23/temperature").kafkaTopic(), is("building_4_room_23"));

        assertThat("The key for building/{building}/room/{room}/# should be building}",
                mapper.map("building/4/room/23/temperature").kafkaKey(), is("building"));

        // Test building/{building}/# pattern
        assertThat("Mqtt pattern building/{building}/# should be mapped to building_{building}_others",
                mapper.map("building/405/room").kafkaTopic(), is("building_405_others"));

        assertThat("Mqtt pattern building/# will be mapped to building_101_others because building/{building}/# was defined before building/#",
                mapper.map("building/101").kafkaTopic(), not("building_others"));

        assertThat("The key for building/{building}/# should be building_{building}",
                mapper.map("building/405/room").kafkaKey(), is("building_405"));

        // Test building/# pattern
        assertThat("Mqtt pattern building/# should be mapped to building_others",
                mapper.map("building").kafkaTopic(), is("building_others"));

        assertThat("The key for building/# should be null",
                mapper.map("building").kafkaKey(), is(nullValue()));

        // Test sensor/# pattern
        assertThat("Mqtt pattern sensor/# should be mapped to sensor_data",
                mapper.map("sensor/temperature").kafkaTopic(), is("sensor_data"));

        assertThat("The key for sensor/# should be sensor",
                mapper.map("sensor/temperature").kafkaKey(), is("sensor"));

        // Test sport/tennis/{player}/# pattern
        assertThat("Mqtt pattern sport/tennis/{player}/# should be mapped to sports_{player}",
                mapper.map("sport/tennis/player1").kafkaTopic(), is("sports_player1"));

        assertThat("Mqtt pattern sport/tennis/{player}/# should be mapped to sports_{player}",
                mapper.map("sport/tennis/player100/ranking").kafkaTopic(), is("sports_player100"));

        assertThat("Mqtt pattern sport/tennis/{player}/# should be mapped to sports_{player}",
                mapper.map("sport/tennis/player123/score/wimbledon").kafkaTopic(), is("sports_player123"));

        assertThat("The key for sport/tennis/{player}/# should be player_{player}",
                mapper.map("sport/tennis/player123/score/wimbledon").kafkaKey(), is("player_player123"));

        // Test +/recipes/# pattern
        assertThat("Mqtt pattern +/recipes/# should be mapped to my_recipes",
                mapper.map("italian/recipes/pizza").kafkaTopic(), is("my_recipes"));

        assertThat("Mqtt pattern +/recipes/# should be mapped to my_recipes",
                mapper.map("italian/recipes/pasta").kafkaTopic(), is("my_recipes"));

        assertThat("Mqtt pattern +/recipes/# should be mapped to my_recipes",
                mapper.map("angolan/recipes/calulu/fish").kafkaTopic(), is("my_recipes"));

        assertThat("The key for +/recipes/# should be null",
                mapper.map("italian/recipes/pizza").kafkaKey(), nullValue());

        // Test {house}/# pattern
        assertThat("Mqtt pattern {house}/# should be mapped to {house}",
                mapper.map("my_house/temperature").kafkaTopic(), is("my_house"));

        assertThat("Mqtt pattern {house}/# should be mapped to {house}",
                mapper.map("my_house/temperature/room1").kafkaTopic(), is("my_house"));

        assertThat("The key for {house}/# should be null",
                mapper.map("my_house/temperature").kafkaKey(), nullValue());
    }
}
