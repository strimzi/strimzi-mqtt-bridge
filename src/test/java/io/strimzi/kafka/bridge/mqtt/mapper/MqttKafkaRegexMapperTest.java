/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;

/**
 * Unit tests for {@link MqttKafkaSimpleMapper}
 */
public class MqttKafkaRegexMapperTest {


    /**
     * Test for default topic.
     * If the MQTT topic does not match any of the mapping rules, the default topic is used.
     * E.g. if the Topic Mapping Rules is empty, the default topic is used by default.
     */
    @Test
    public void testDefaultTopic() {
        List<MappingRule> rules = new ArrayList<>();

        MqttKafkaRegexMapper mapper = new MqttKafkaRegexMapper(rules);

        assertThat("Should use the default topic when no mapping pattern matches.",
                mapper.map("sensor/temperature").kafkaTopic(), is(MqttKafkaSimpleMapper.DEFAULT_KAFKA_TOPIC));

        assertThat("The key for the default topic should be null",
                mapper.map("sensor/temperature").kafkaKey(), nullValue());
    }

    /**
     * Test the mapping of single level topics.
     */
    @Test
    public void testSingleLevel() {
        List<MappingRule> rules = new ArrayList<>();

        rules.add(new MappingRule("sensors/[^/]+/data", "sensor_data", null));
        rules.add(new MappingRule("devices/([^/]+)/data", "devices_$1_data", "device_$1"));
        rules.add(new MappingRule("fleet/([0-9]+)/vehicle/(\\w+)", "fleet_$1", "fleet$2"));
        rules.add(new MappingRule("building/(\\d+)/floor/(\\d+).*", "building_$1_$2", "building_$1"));
        rules.add(new MappingRule("term/(\\d+)", "term$1", "my_term_key_$1"));
        rules.add(new MappingRule("devices/([^/]+)/data/(\\b(all|new|old)\\b)", "devices_$1_$2_data", "device_$1"));

        MqttKafkaRegexMapper mapper = new MqttKafkaRegexMapper(rules);

        assertThat("building/(\\d+)/floor/(\\d+).* should be mapped to building_$1_$2",
                mapper.map("building/14/floor/25").kafkaTopic(), is("building_14_25"));

        assertThat("The key for building_$1 should be expanded to building_14",
                mapper.map("building/14/floor/25").kafkaKey(), is("building_14"));

        assertThat("sensors/[^/]+/data should be mapped to sensor_data",
                mapper.map("sensors/temperature/data").kafkaTopic(), is("sensor_data"));

        assertThat("The key for the rule sensors/[^/]+/data should be null",
                mapper.map("sensors/temperature/data").kafkaKey(), nullValue());

        assertThat("devices/[^/]+/data should be mapped to devices_$1_data",
                mapper.map("devices/123/data").kafkaTopic(), is("devices_123_data"));

        assertThat("The key for devices/[^/]+/data should be device_$1",
                mapper.map("devices/123/data").kafkaKey(), is("device_123"));

        assertThat("fleet/([0-9]+)/vehicle/(\\w+) should be mapped to fleet_$1",
                mapper.map("fleet/4/vehicle/23").kafkaTopic(), is("fleet_4"));

        assertThat("The key for fleet/([0-9]+)/vehicle/(\\w+) should be fleet$2",
                mapper.map("fleet/4/vehicle/23").kafkaKey(), is("fleet23"));

        assertThat("term/(\\d+) should be mapped to term$1",
                mapper.map("term/4").kafkaTopic(), is("term4"));

        assertThat("The key for term/(\\d+) should be my_term_key_$1",
                mapper.map("term/4").kafkaKey(), is("my_term_key_4"));

        assertThat("devices/([^/]+)/data/(\\b(all|new|old)\\b) should be mapped to devices_$1_data",
                mapper.map("devices/bluetooth/data/all").kafkaTopic(), is("devices_bluetooth_all_data"));
    }


    /**
     * Test the mapping of single level topics.
     */
    @Test
    public void testIllegalPlaceholder() {

        List<MappingRule> rules = new ArrayList<>();
        rules.add(new MappingRule("fleet/vehicle/(\\d+)", "fleet_$1", "fleet_$2_$1"));
        rules.add(new MappingRule("buildings/([^/]+)/rooms/([^/]+)/device/([^/]+)", "buildings_$0_rooms_$1_device_$2", null));
        rules.add(new MappingRule("building/(\\d{1,2})/room/(\\d{1,4})", "building_$1_room_$2_$3", null));

        MqttKafkaRegexMapper mapper = new MqttKafkaRegexMapper(rules);

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

        rules.add(new MappingRule("building/([^/]+)/room/(\\d{1,3}).*", "building_$1_room_$2", "building_$1"));
        rules.add(new MappingRule("building/([^/]+).*", "building_$1_others", "$1"));
        rules.add(new MappingRule("building.*", "building_others", null));
        rules.add(new MappingRule("fleet/([0-9])/vehicle/(\\w+).*", "fleet_$1", "fleet$1"));
        rules.add(new MappingRule("sensor.*", "sensor_data", "sensor"));
        rules.add(new MappingRule("sport/tennis/(\\w+).*", "sports_$1", null));
        rules.add(new MappingRule("(\\w+)/recipes.*", "$1_recipes", "$1"));
        rules.add(new MappingRule("([^/]+).*", "$1", null));

        MqttKafkaRegexMapper mapper = new MqttKafkaRegexMapper(rules);

        // Test for building/([^/]+)/room/(\\d{1,3}).* pattern
        assertThat("Mqtt pattern building/([^/]+)/room/(\\d{1,3}).* should be mapped to building_$1_room_$2",
                mapper.map("building/4/room/23/temperature").kafkaTopic(), is("building_4_room_23"));

        assertThat("The key for building/([^/]+)/room/(\\d{1,3}).* should be expanded to building_$1",
                mapper.map("building/4/room/23/temperature").kafkaKey(), is("building_4"));

        // Test for building/([^/]+).* pattern

        assertThat("Mqtt pattern building/([^/]+).* should be mapped to building_$1_others",
                mapper.map("building/405/room").kafkaTopic(), is("building_405_others"));

        assertThat("The key for building/([^/]+).* should be expanded to building_$1",
                mapper.map("building/405/room").kafkaKey(), not("4"));

        // Test for building.* pattern
        assertThat("Mqtt pattern building.* will be mapped to building_101_others because building/([^/]+).* was defined before building.*",
                mapper.map("building/101").kafkaTopic(), not("building_others"));

        assertThat("building.* should be mapped to building_others",
                mapper.map("building").kafkaTopic(), is("building_others"));

        // Test for fleet/([0-9])/vehicle/(\\w+).* pattern
        assertThat("Mqtt pattern fleet/([0-9])/vehicle/(\\w+).* should be mapped to fleet_$1",
                mapper.map("fleet/9/vehicle/23/velocity").kafkaTopic(), is("fleet_9"));

        assertThat("The key for fleet/([0-9])/vehicle/(\\w+).* should be expanded to fleet$1",
                mapper.map("fleet/9/vehicle/23/velocity").kafkaKey(), is("fleet9"));

        // Test for sensor.* pattern
        assertThat("Mqtt pattern sensor.* should be mapped to sensor_data",
                mapper.map("sensors/temperature/data").kafkaTopic(), is("sensor_data"));

        assertThat("The key for sensor.* should be expanded to sensor",
                mapper.map("sensors/temperature/data").kafkaKey(), is("sensor"));

        // Test for sport/tennis/(\\w+).* pattern
        assertThat("Mqtt pattern sport/tennis/(\\w+).* should be mapped to sports_$1",
                mapper.map("sport/tennis/player1").kafkaTopic(), is("sports_player1"));

        assertThat("Mqtt pattern sport/tennis/(\\w+).* should be mapped to sports_$1",
                mapper.map("sport/tennis/player100/ranking").kafkaTopic(), is("sports_player100"));

        assertThat("Mqtt pattern sport/tennis/(\\w+).* should be mapped to sports_$1",
                mapper.map("sport/tennis/player123/score/wimbledon").kafkaTopic(), is("sports_player123"));

        // Test for ([^/]+)/recipes.* pattern
        assertThat("Mqtt pattern ([^/]+)/recipes.* should be mapped to my_recipes",
                mapper.map("angolan/recipes/caculu/fish").kafkaTopic(), is("angolan_recipes"));

        assertThat("The key for ([^/]+)/recipes.* should be expanded to $1",
                mapper.map("angolan/recipes/caculu/fish").kafkaKey(), is("angolan"));

        // Test for ([^/]+).* pattern
        assertThat("Mqtt pattern ([^/]+).* should be mapped to $1",
                mapper.map("my_house/temperature").kafkaTopic(), is("my_house"));
    }

    /**
     * Test mapping with incorrect regex.
     */
    @Test
    public void testIncorrectRegex() {
        List<MappingRule> rules = new ArrayList<>();

        rules.add(new MappingRule("building/([^/]+)/room/(\\d{1,3}).*", "building_$1_room_$2", "building_$1"));
        rules.add(new MappingRule("building/(\\p).*", "building_$1_others", "$1"));
        rules.add(new MappingRule("building.*", "building_others", null));

        Exception mapper = assertThrows(PatternSyntaxException.class, () -> new MqttKafkaRegexMapper(rules));

        assertThat("Should throw PatternSyntaxException",
                mapper.getMessage(), startsWith("Unknown character property name {)} near index 12"));
    }

    /**
     * Test the order of the regex.
     */
    @Test
    public void testRegexOrder() {
        List<MappingRule> rules = new ArrayList<>();

        rules.add(new MappingRule("home/(\\w+)/temperature/(sensor\\d{1,2})/readings/(\\b(all|new|old)\\b)", "temperature_$2_in_$1", "$2_$3_data_from_$1"));
        rules.add(new MappingRule("sports/([^/]+)/league/season/(\\d{4}-\\d{4})/match\\/(\\d+)\\/goal\\/(\\d+)", "season_$2_$1", "$1"));

        MqttKafkaRegexMapper mapper = new MqttKafkaRegexMapper(rules);

        assertThat("Mqtt pattern home/(\\w+)/temperature/(sensor\\d{1,2})/readings/(\\b(all|new|old)\\b) should be mapped to temperature_$2_in_$1",
                mapper.map("home/bedroom/temperature/sensor01/readings/all").kafkaTopic(), is("temperature_sensor01_in_bedroom"));

        assertThat("The key for home/(\\w+)/temperature/(sensor\\d{1,2})/readings/(\\b(all|new|old)\\b) should be expanded to $2_$3_data_from_$1",
                mapper.map("home/bedroom/temperature/sensor01/readings/all").kafkaKey(), is("sensor01_all_data_from_bedroom"));

        assertThat("Mqtt pattern sports/([^/]+)/league/season/(\\d{4}-\\d{4})/match\\/(\\d+)\\/goal\\/(\\d+) should be mapped to season_$2_$1",
                mapper.map("sports/baseball/league/season/2019-2020/match/1/goal/1").kafkaTopic(), is("season_2019-2020_baseball"));

        assertThat("The key for sports/([^/]+)/league/season/(\\d{4}-\\d{4})/match\\/(\\d+)\\/goal\\/(\\d+) should be expanded to $1",
                mapper.map("sports/baseball/league/season/2019-2020/match/1/goal/1").kafkaKey(), is("baseball"));

    }
}
