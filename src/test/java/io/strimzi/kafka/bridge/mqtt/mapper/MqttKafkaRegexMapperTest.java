/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link MqttKafkaRegexMapper}
 */
public class MqttKafkaRegexMapperTest {
    private final String defaultTopicTest = "default_topic";

    /**
     * Test for default topic.
     * If the MQTT topic does not match any of the mapping rules, the default topic is used.
     * E.g. if the Topic Mapping Rules is empty, the default topic is used by default.
     */
    @Test
    public void testDefaultTopic() {
        List<MappingRule> rules = new ArrayList<>();

        MqttKafkaRegexMapper mapper = new MqttKafkaRegexMapper(rules, defaultTopicTest);

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

        rules.add(new MappingRule("sensors/[^/]+/data", "sensor_data", null));
        rules.add(new MappingRule("devices/([^/]+)/data", "devices_$1_data", null));
        rules.add(new MappingRule("fleet/([0-9]+)/vehicle/(\\w+)", "fleet_$1", "vehicle$2"));
        rules.add(new MappingRule("building/(\\d+)/floor/(\\d+)", "building_$1", "floor_$2"));
        rules.add(new MappingRule("term/(\\d+)", "term$1", null));
        rules.add(new MappingRule("devices/([^/]+)/data/(\\b(all|new|old)\\b)", "devices_$1_data", "devices_$2"));

        MqttKafkaRegexMapper mapper = new MqttKafkaRegexMapper(rules, defaultTopicTest);

        // Test building/(\\d+)/floor/(\\d+)
        MappingResult mappingResult = mapper.map("building/14/floor/25");

        assertThat("building/(\\d+)/floor/(\\d+) should be mapped to building_$1",
                mappingResult.kafkaTopic(), is("building_14"));

        assertThat("The key for floor_$2 should be expanded to floor_25",
                mappingResult.kafkaKey(), is("floor_25"));

        // Test sensors/[^/]+/data
        mappingResult = mapper.map("sensors/temperature/data");

        assertThat("sensors/[^/]+/data should be mapped to sensor_data",
                mappingResult.kafkaTopic(), is("sensor_data"));

        assertThat("The key for the rule sensors/[^/]+/data should be null",
                mappingResult.kafkaKey(), nullValue());

        // Test devices/([^/]+)/data
        mappingResult = mapper.map("devices/123/data");

        assertThat("devices/[^/]+/data should be mapped to devices_$1_data",
                mappingResult.kafkaTopic(), is("devices_123_data"));

        assertThat("The key for devices/[^/]+/data should be null",
                mappingResult.kafkaKey(), nullValue());

        // Test fleet/([0-9]+)/vehicle/(\\w+)
        mappingResult = mapper.map("fleet/4/vehicle/23");

        assertThat("fleet/([0-9]+)/vehicle/(\\w+) should be mapped to fleet_$1",
                mappingResult.kafkaTopic(), is("fleet_4"));

        assertThat("The key for fleet/([0-9]+)/vehicle/(\\w+) should be vehicle$2",
                mappingResult.kafkaKey(), is("vehicle23"));

        // Test term/(\\d+)
        mappingResult = mapper.map("term/4");

        assertThat("term/(\\d+) should be mapped to term$1",
                mappingResult.kafkaTopic(), is("term4"));

        assertThat("The key for term/(\\d+) should be null",
                mappingResult.kafkaKey(), nullValue());

        // Test devices/([^/]+)/data/(\\b(all|new|old)\\b)
        mappingResult = mapper.map("devices/bluetooth/data/all");

        assertThat("devices/([^/]+)/data/(\\b(all|new|old)\\b) should be mapped to devices_$1_data",
                mappingResult.kafkaTopic(), is("devices_bluetooth_data"));

        assertThat("The key for devices/([^/]+)/data/(\\b(all|new|old)\\b) should be devices_$2",
                mappingResult.kafkaKey(), is("devices_all"));
    }


    /**
     * Test the mapping of single level topics.
     */
    @Test
    public void testIllegalPlaceholder() {

        List<MappingRule> rules = new ArrayList<>();
        rules.add(new MappingRule("fleet/vehicle/(\\d+)", "fleet_$1", "fleet_$2"));
        rules.add(new MappingRule("buildings/([^/]+)/rooms/([^/]+)/device/([^/]+)", "buildings_$0_rooms_$1_device_$2", "device_$3"));
        rules.add(new MappingRule("building/(\\d{1,2})/room/(\\d{1,4})", "building_$1_room_$2_$3", "room_$4"));

        MqttKafkaRegexMapper mapper = new MqttKafkaRegexMapper(rules, defaultTopicTest);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> mapper.map("fleet/vehicle/23"));

        String expectedMessage = "The placeholder $2 was not found or assigned any value.";
        assertThat("The exception message should be: " + expectedMessage,
                exception.getMessage(), is(expectedMessage));

        Exception otherException = assertThrows(IllegalArgumentException.class, () -> mapper.map("buildings/10/rooms/5/device/3"));

        String otherExpectedMessage = "The placeholder $0 was not found or assigned any value.";
        assertThat("The exception message should be: " + otherExpectedMessage,
                otherException.getMessage(), is(otherExpectedMessage));

        Exception anotherException = assertThrows(IllegalArgumentException.class, () -> mapper.map("building/10/room/403"));

        String anotherExpectedMessage = "The placeholder $3 was not found or assigned any value.";
        assertThat("The exception message should be: " + anotherExpectedMessage,
                anotherException.getMessage(), is(anotherExpectedMessage));
    }

    /**
     * Test the mapping of multi level topics.
     */
    @Test
    public void testMultiLevel() {
        List<MappingRule> rules = new ArrayList<>();

        rules.add(new MappingRule("building/([^/]+)/room/(\\d{1,3}).*", "building_$1", "room_$2"));
        rules.add(new MappingRule("building/([^/]+).*", "building_$1_others", null));
        rules.add(new MappingRule("building.*", "building_others", null));
        rules.add(new MappingRule("fleet/([0-9])/vehicle/(\\w+)(?:\\/.*)?$", "fleet_$1", "vehicle_$2"));
        rules.add(new MappingRule("sensor.*", "sensor_data", null));
        rules.add(new MappingRule("sport/tennis/(\\w+).*", "sports_$1", null));
        rules.add(new MappingRule("(\\w+)/recipes(?:\\/.*)?$", "$1_recipes", null));
        rules.add(new MappingRule("([^/]+)/.*", "$1", null));

        MqttKafkaRegexMapper mapper = new MqttKafkaRegexMapper(rules, defaultTopicTest);


        // Test for building/([^/]+)/room/(\\d{1,3}).* pattern
        MappingResult mappingResult = mapper.map("building/4/room/23/temperature");

        assertThat("Mqtt pattern building/([^/]+)/room/(\\d{1,3}).* should be mapped to building_$1",
                mappingResult.kafkaTopic(), is("building_4"));

        assertThat("The key for building/([^/]+)/room/(\\d{1,3}).* should be expanded to room_$2",
                mappingResult.kafkaKey(), is("room_23"));

        // Test for building/([^/]+).* pattern
        mappingResult = mapper.map("building/405/room");

        assertThat("Mqtt pattern building/([^/]+).* should be mapped to building_$1_others",
                mappingResult.kafkaTopic(), is("building_405_others"));

        assertThat("The key for building/([^/]+).* should be expanded to null",
                mappingResult.kafkaKey(), nullValue());

        // Test for building.* pattern
        mappingResult = mapper.map("building/101");

        assertThat("Mqtt pattern building.* will be mapped to building_101_others because building/([^/]+).* was defined before building.*",
                mappingResult.kafkaTopic(), not("building_others"));

        assertThat("Mqtt pattern building.* will be mapped to building_101_others because building/([^/]+).* was defined before building.*",
                mappingResult.kafkaTopic(), is("building_101_others"));

        assertThat("The key for building.* should be expanded to null",
                mappingResult.kafkaKey(), nullValue());

        assertThat("building.* should be mapped to building_others",
                mapper.map("building").kafkaTopic(), is("building_others"));

        // Test for fleet/([0-9])/vehicle/(\\w+)(?:\/.*)?$ pattern
        mappingResult = mapper.map("fleet/9/vehicle/23/velocity");

        assertThat("Mqtt pattern fleet/([0-9])/vehicle/(\\w+)(?:\\/.*)?$ should be mapped to fleet_$1",
                mappingResult.kafkaTopic(), is("fleet_9"));

        assertThat("The key for fleet/([0-9])/vehicle/(\\w+)(?:\\/.*)?$ should be expanded to fleet$1",
                mappingResult.kafkaKey(), is("vehicle_23"));

        // Test for sensor.* pattern
        mappingResult = mapper.map("sensor/temperature/data");

        assertThat("Mqtt pattern sensor.* should be mapped to sensor_data",
                mappingResult.kafkaTopic(), is("sensor_data"));

        assertThat("The key for sensor.* should be expanded to null",
                mappingResult.kafkaKey(), nullValue());

        // Test for sport/tennis/(\\w+).* pattern
        mappingResult = mapper.map("sport/tennis/player123/score/wimbledon");

        assertThat("Mqtt pattern sport/tennis/(\\w+).* should be mapped to sports_$1",
                mappingResult.kafkaTopic(), is("sports_player123"));

        assertThat("The key for sport/tennis/(\\w+).* should be expanded to null",
                mappingResult.kafkaKey(), nullValue());

        assertThat("Mqtt pattern sport/tennis/(\\w+).* should be mapped to sports_$1",
                mapper.map("sport/tennis/player1").kafkaTopic(), is("sports_player1"));

        assertThat("Mqtt pattern sport/tennis/(\\w+).* should be mapped to sports_$1",
                mapper.map("sport/tennis/player100/ranking").kafkaTopic(), is("sports_player100"));

        // Test for ([^/]+)/recipes(?:\/.*)?$ pattern
        mappingResult = mapper.map("angolan/recipes/caculu/fish");

        assertThat("Mqtt pattern ([^/]+)/recipes(?:\\/.*)?$ should be mapped to my_recipes",
                mappingResult.kafkaTopic(), is("angolan_recipes"));

        assertThat("The key for ([^/]+)/recipes(?:\\/.*)?$ should be expanded to null",
                mappingResult.kafkaKey(), nullValue());

        // Test for ([^/]+)/.* pattern
        mappingResult = mapper.map("my_house/temperature");

        assertThat("Mqtt pattern ([^/]+)/.* should be mapped to $1",
                mappingResult.kafkaTopic(), is("my_house"));

        assertThat("The key for ([^/]+).* should be expanded to null",
                mappingResult.kafkaKey(), nullValue());
    }

    /**
     * Test mapping with incorrect regex.
     */
    @Test
    public void testIncorrectRegex() {
        List<MappingRule> rules = new ArrayList<>();

        rules.add(new MappingRule("building/([^/]+)/room/(\\d{1,3}).*", "building_$1", "room_$1"));
        rules.add(new MappingRule("building/(\\p).*", "building_$1_others", null));
        rules.add(new MappingRule("building.*", "building_others", null));

        Exception mapper = assertThrows(PatternSyntaxException.class, () -> new MqttKafkaRegexMapper(rules, defaultTopicTest));

        assertThat("Should throw PatternSyntaxException",
                mapper.getMessage(), startsWith("Unknown character property name {)} near index 12"));

        // test for .* in capture groups
        rules.add(0, new MappingRule("fleet/([0-9])/vehicle/(\\w+)/(.*)", "fleet_$1", "vehicle_$2"));

        assertThrows(IllegalArgumentException.class, () -> new MqttKafkaRegexMapper(rules, defaultTopicTest));
    }

    /**
     * Test the order of the regex.
     */
    @Test
    public void testRegexOrder() {
        List<MappingRule> rules = new ArrayList<>();

        rules.add(new MappingRule("home/(\\w+)/temperature/(sensor\\d{1,2})/readings/(\\b(all|new|old)\\b)", "temperature_$2_in_$1", "$3"));
        rules.add(new MappingRule("sports/([^/]+)/league/season/(\\d{4}-\\d{4})/match/(\\d+).*", "season_$2_$1", "match_$3"));

        MqttKafkaRegexMapper mapper = new MqttKafkaRegexMapper(rules, defaultTopicTest);

        // Test home/(\\w+)/temperature/(sensor\\d{1,2})/readings/(\\b(all|new|old)\\b) pattern
        MappingResult mappingResult = mapper.map("home/bedroom/temperature/sensor01/readings/all");

        assertThat("Mqtt pattern home/(\\w+)/temperature/(sensor\\d{1,2})/readings/(\\b(all|new|old)\\b) should be mapped to temperature_$2_in_$1",
                mappingResult.kafkaTopic(), is("temperature_sensor01_in_bedroom"));

        assertThat("The key for home/(\\w+)/temperature/(sensor\\d{1,2})/readings/(\\b(all|new|old)\\b) should be expanded to $3",
                mapper.map("home/bedroom/temperature/sensor01/readings/all").kafkaKey(), is("all"));

        // Test sports/([^/]+)/league/season/(\\d{4}-\\d{4})/match/(\\d+).* pattern
        mappingResult = mapper.map("sports/baseball/league/season/2019-2020/match/1/goal/1");

        assertThat("Mqtt pattern sports/([^/]+)/league/season/(\\d{4}-\\d{4})/match/(\\d+)/goal/(\\d+).* should be mapped to season_$2_$1",
                mappingResult.kafkaTopic(), is("season_2019-2020_baseball"));

        assertThat("The key for sports/([^/]+)/league/season/(\\d{4}-\\d{4})/match/(\\d+)/goal/(\\d+).* should be expanded to match_$3",
                mappingResult.kafkaKey(), is("match_1"));

    }
}
