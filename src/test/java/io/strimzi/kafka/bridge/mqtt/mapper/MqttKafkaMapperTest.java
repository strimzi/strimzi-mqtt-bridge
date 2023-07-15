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
                mapper.map("sensor/temperature").getKafkaTopic(), is(MqttKafkaMapper.DEFAULT_KAFKA_TOPIC));

        assertThat("The key for the default topic should be null",
                mapper.map("sensor/temperature").getKafkaKey(), nullValue());
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
        rules.add(new MappingRule("building/(\\d+)/floor/(\\d+).*", "building_$2_$1", "building_$1"));
        rules.add(new MappingRule("term/(\\d+)", "term$1", "my_term_key_$1"));
        rules.add(new MappingRule("devices/([^/]+)/data/(\\b(all|new|old)\\b)", "devices_$1_$2_data", "device_$1"));

        MqttKafkaMapper mapper = new MqttKafkaMapper(rules);
        MappingResult mappingResult = mapper.map("building/4/floor/25");

        assertThat("building/(\\d+)/floor/(\\d+).* should be mapped to building_$2_$1",
                mappingResult.getKafkaTopic(), is("building_25_4"));

        assertThat("The key for building_$2 should be expanded to building_25",
                mappingResult.getKafkaKey(), is("building_4"));

        mappingResult = mapper.map("sensors/temperature/data");

        assertThat("sensors/[^/]+/data should be mapped to sensor_data",
                mappingResult.getKafkaTopic(), is("sensor_data"));

        assertThat("The key for the rule sensors/[^/]+/data should be null",
                mappingResult.getKafkaKey(), nullValue());

        mappingResult = mapper.map("devices/123/data");

        assertThat("devices/[^/]+/data should be mapped to devices_$1_data",
                mappingResult.getKafkaTopic(), is("devices_123_data"));

        assertThat("The key for devices/[^/]+/data should be device_$1",
                mappingResult.getKafkaKey(), is("device_123"));

        mappingResult = mapper.map("fleet/4/vehicle/23");

        assertThat("fleet/([0-9]+)/vehicle/(\\w+) should be mapped to fleet_$1",
                mappingResult.getKafkaTopic(), is("fleet_4"));

        assertThat("The key for fleet/([0-9]+)/vehicle/(\\w+) should be fleet$2",
                mappingResult.getKafkaKey(), is("fleet23"));

        mappingResult = mapper.map("term/4");

        assertThat("term/(\\d+) should be mapped to term$1",
                mappingResult.getKafkaTopic(), is("term4"));

        assertThat("The key for term/(\\d+) should be my_term_key_$1",
                mappingResult.getKafkaKey(), is("my_term_key_4"));

        assertThat("devices/([^/]+)/data/(\\b(all|new|old)\\b) should be mapped to devices_$1_data",
                mapper.map("devices/bluetooth/data/all").getKafkaTopic(), is("devices_bluetooth_all_data"));
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

        MqttKafkaMapper mapper = new MqttKafkaMapper(rules);

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

        MqttKafkaMapper mapper = new MqttKafkaMapper(rules);

        // Test for building/([^/]+)/room/(\\d{1,3}).* pattern
        MappingResult mappingResult = mapper.map("building/4/room/23/temperature");

        assertThat("Mqtt pattern building/([^/]+)/room/(\\d{1,3}).* should be mapped to building_$1_room_$2",
                mappingResult.getKafkaTopic(), is("building_4_room_23"));

        assertThat("The key for building/([^/]+)/room/(\\d{1,3}).* should be expanded to building_$1",
                mappingResult.getKafkaKey(), is("building_4"));

        // Test for building/([^/]+).* pattern
        mappingResult = mapper.map("building/405/room");

        assertThat("Mqtt pattern building/([^/]+).* should be mapped to building_$1_others",
                mappingResult.getKafkaTopic(), is("building_405_others"));

        assertThat("The key for building/([^/]+).* should be expanded to building_$1",
                mappingResult.getKafkaKey(), not("4"));

        // Test for building.* pattern
        mappingResult = mapper.map("building/101");

        assertThat("Mqtt pattern building.* will be mapped to building_101_others because building/([^/]+).* was defined before building.*",
                mappingResult.getKafkaTopic(), not("building_others"));

        assertThat("building.* should be mapped to building_others",
                mapper.map("building").getKafkaTopic(), is("building_others"));

        // Test for fleet/([0-9])/vehicle/(\\w+).* pattern
        mappingResult = mapper.map("fleet/9/vehicle/23/velocity");

        assertThat("Mqtt pattern fleet/([0-9])/vehicle/(\\w+).* should be mapped to fleet_$1",
                mappingResult.getKafkaTopic(), is("fleet_9"));

        assertThat("The key for fleet/([0-9])/vehicle/(\\w+).* should be expanded to fleet$1",
                mappingResult.getKafkaKey(), is("fleet9"));

        // Test for sensor.* pattern
        mappingResult = mapper.map("sensors/temperature/data");

        assertThat("Mqtt pattern sensor.* should be mapped to sensor_data",
                mappingResult.getKafkaTopic(), is("sensor_data"));

        assertThat("The key for sensor.* should be expanded to sensor",
                mappingResult.getKafkaKey(), is("sensor"));

        // Test for sport/tennis/(\\w+).* pattern
        assertThat("Mqtt pattern sport/tennis/(\\w+).* should be mapped to sports_$1",
                mapper.map("sport/tennis/player1").getKafkaTopic(), is("sports_player1"));

        assertThat("Mqtt pattern sport/tennis/(\\w+).* should be mapped to sports_$1",
                mapper.map("sport/tennis/player100/ranking").getKafkaTopic(), is("sports_player100"));

        assertThat("Mqtt pattern sport/tennis/(\\w+).* should be mapped to sports_$1",
                mapper.map("sport/tennis/player123/score/wimbledon").getKafkaTopic(), is("sports_player123"));

        // Test for ([^/]+)/recipes.* pattern
        mappingResult = mapper.map("angolan/recipes/caculu/fish");

        assertThat("Mqtt pattern ([^/]+)/recipes.* should be mapped to my_recipes",
                mappingResult.getKafkaTopic(), is("angolan_recipes"));

        assertThat("The key for ([^/]+)/recipes.* should be expanded to $1",
                mappingResult.getKafkaKey(), is("angolan"));

        // Test for ([^/]+).* pattern
        mappingResult = mapper.map("my_house/temperature");

        assertThat("Mqtt pattern ([^/]+).* should be mapped to $1",
                mappingResult.getKafkaTopic(), is("my_house"));
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

        Exception mapper = assertThrows(PatternSyntaxException.class, () -> new MqttKafkaMapper(rules));

        assertThat("Should throw PatternSyntaxException",
                mapper.getMessage(), startsWith("Unknown character property name {)} near index 12"));
    }
}
