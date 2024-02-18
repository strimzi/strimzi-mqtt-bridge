/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for @{@link MqttKafkaSimpleMapper}.
 *
 * @see MqttKafkaSimpleMapper
 */
public class MqttKafkaSimpleMapperTest {

    private final String defaultTopicTest = "default_topic";
    /**
     * Test for default topic.
     * If the MQTT topic does not match any of the mapping rules, the default topic is used.
     * E.g. if the Topic Mapping Rules is empty, the default topic is used by default.
     */
    @Test
    public void testDefaultTopic() {
        List<MappingRule> rules = new ArrayList<>();

        MqttKafkaSimpleMapper mapper = new MqttKafkaSimpleMapper(rules, defaultTopicTest);

        MappingResult result = mapper.map("sensor/temperature");
        assertThat("Should use the default topic when no mapping pattern matches.",
                result.kafkaTopic(), is(defaultTopicTest));

        assertThat("The key for the default topic should be null",
                result.kafkaKey(), nullValue());
    }

    /**
     * Test the mapping of single level topics.
     */
    @Test
    public void testSingleLevel() {
        List<MappingRule> rules = new ArrayList<>();

        rules.add(new MappingRule("sensors/+/data", "sensor_data", null));
        rules.add(new MappingRule("devices/{device}/data", "devices_{device}_data", null));
        rules.add(new MappingRule("fleet/{fleet}/vehicle/{vehicle}", "fleet_{fleet}", "vehicle_{vehicle}"));
        rules.add(new MappingRule("building/{building}/floor/{floor}", "building.{building}", "floor_{floor}"));
        rules.add(new MappingRule("term/{number}", "term{number}", null));

        MqttKafkaSimpleMapper mapper = new MqttKafkaSimpleMapper(rules, defaultTopicTest);

        // Test sensors/+/data
        MappingResult mappingResult = mapper.map("sensors/8/data");

        assertThat("Mqtt pattern sensors/+/data should be mapped to sensor_data",
                mappingResult.kafkaTopic(), is("sensor_data"));

        assertThat("The key for sensors/+/data should be null",
                mappingResult.kafkaKey(), nullValue());

        // Test devices/{device}/data
        mappingResult = mapper.map("devices/4/data");

        assertThat("Mqtt pattern devices/{device}/data should be mapped to devices_{device}_data",
                mappingResult.kafkaTopic(), is("devices_4_data"));

        assertThat("The key for devices/{device}/data should be null",
                mappingResult.kafkaKey(), nullValue());

        // Test fleet/{fleet}/vehicle/{vehicle}
        mappingResult = mapper.map("fleet/4/vehicle/23");

        assertThat("Mqtt pattern fleet/{fleet}/vehicle/{vehicle} should be mapped to fleet_{fleet}",
                mappingResult.kafkaTopic(), is("fleet_4"));

        assertThat("The key for fleet/{fleet}/vehicle/{vehicle} should be vehicle_{vehicle}",
                mappingResult.kafkaKey(), is("vehicle_23"));

        // Test building/{building}/floor/{floor}
        mappingResult = mapper.map("building/40/floor/13");

        assertThat("building/{building}/floor/{floor} should be mapped to building.{building}",
                mappingResult.kafkaTopic(), is("building.40"));

        assertThat("The key for building/{building}/floor/{floor} should be floor_{floor}",
                mappingResult.kafkaKey(), is("floor_13"));

        // Test term/{number}
        mappingResult = mapper.map("term/4");

        assertThat("Mqtt pattern term/{number} should be mapped to term{number}",
                mappingResult.kafkaTopic(), is("term4"));

        assertThat("The key for term/{number} should be null",
                mappingResult.kafkaKey(), nullValue());
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

        MqttKafkaSimpleMapper mapper = new MqttKafkaSimpleMapper(rules, defaultTopicTest);

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

        rules.add(new MappingRule("building/{building}/room/{room}/#", "building_{building}", "room_{room}"));
        rules.add(new MappingRule("building/{building}/#", "building_{building}_others", null));
        rules.add(new MappingRule("building/#", "building_others", null));
        rules.add(new MappingRule("fleet/{fleet}/vehicle/{vehicle}/#", "fleet_{fleet}", "vehicle_{vehicle}"));
        rules.add(new MappingRule("sensor/#", "sensor_data", null));
        rules.add(new MappingRule("sport/tennis/{player}/#", "sports_{player}", null));
        rules.add(new MappingRule("+/recipes/#", "my_recipes", null));
        rules.add(new MappingRule("{house}/#", "{house}", null));

        MqttKafkaSimpleMapper mapper = new MqttKafkaSimpleMapper(rules, defaultTopicTest);

        // Test fleet/{fleet}/vehicle/{vehicle}/# pattern
        MappingResult mappingResult = mapper.map("fleet/4/vehicle/23/velocity");

        assertThat("Mqtt topic pattern fleet/{fleet}/vehicle/{vehicle}/# should be mapped to fleet_{fleet}",
                mappingResult.kafkaTopic(), is("fleet_4"));

        assertThat("The key for fleet/{fleet}/vehicle/{vehicle}/# should be vehicle_{vehicle}",
                mappingResult.kafkaKey(), is("vehicle_23"));

        // Test building/{building}/room/{room}/# pattern
        mappingResult = mapper.map("building/4/room/23/temperature");

        assertThat("Mqtt pattern building/{building}/room/{room}/# should be mapped to building_{building}_room_{room}",
                mappingResult.kafkaTopic(), is("building_4"));

        assertThat("The key for building/{building}/room/{room}/# should be room_{room}",
                mappingResult.kafkaKey(), is("room_23"));

        // Test building/{building}/# pattern
        mappingResult = mapper.map("building/405/room");

        assertThat("Mqtt pattern building/{building}/# should be mapped to building_{building}_others",
                mappingResult.kafkaTopic(), is("building_405_others"));

        assertThat("The key for building/{building}/# should be null",
                mappingResult.kafkaKey(), nullValue());

        // Test building/# pattern
        mappingResult = mapper.map("building");
        assertThat("Mqtt pattern building/# should be mapped to building_others",
                mappingResult.kafkaTopic(), is("building_others"));

        assertThat("The key for building/# should be null",
                mappingResult.kafkaKey(), nullValue());

        assertThat("Mqtt pattern building/# will be mapped to building_101_others because building/{building}/# was defined before building/#",
                mapper.map("building/101").kafkaTopic(), not("building_others"));

        // Test sensor/# pattern
        mappingResult = mapper.map("sensor/temperature");

        assertThat("Mqtt pattern sensor/# should be mapped to sensor_data",
                mappingResult.kafkaTopic(), is("sensor_data"));

        assertThat("The key for sensor/# should be null",
                mappingResult.kafkaKey(), nullValue());

        // Test sport/tennis/{player}/# pattern
        mappingResult = mapper.map("sport/tennis/player1/ranking");

        assertThat("Mqtt pattern sport/tennis/{player}/# should be mapped to sports_{player}",
                mappingResult.kafkaTopic(), is("sports_player1"));

        mappingResult = mapper.map("sport/tennis/player123/score/wimbledon");
        assertThat("Mqtt pattern sport/tennis/{player}/# should be mapped to sports_{player}",
                mappingResult.kafkaTopic(), is("sports_player123"));

        assertThat("The key for sport/tennis/{player}/# should be null",
                mappingResult.kafkaKey(), nullValue());

        // Test +/recipes/# pattern
        mappingResult = mapper.map("italian/recipes/pizza");

        assertThat("Mqtt pattern +/recipes/# should be mapped to my_recipes",
                mappingResult.kafkaTopic(), is("my_recipes"));

        assertThat("The key for +/recipes/# should be null",
                mappingResult.kafkaKey(), nullValue());

        mappingResult = mapper.map("angolan/recipes/calulu/fish");

        assertThat("Mqtt pattern +/recipes/# should be mapped to my_recipes",
                mappingResult.kafkaTopic(), is("my_recipes"));

        // Test {house}/# pattern
        mappingResult = mapper.map("my_house/temperature");
        assertThat("Mqtt pattern {house}/# should be mapped to {house}",
                mappingResult.kafkaTopic(), is("my_house"));

        assertThat("The key for {house}/# should be null",
                mappingResult.kafkaKey(), nullValue());

        assertThat("Mqtt pattern {house}/# should be mapped to {house}",
                mapper.map("my_house/temperature/room1").kafkaTopic(), is("my_house"));
    }
}
