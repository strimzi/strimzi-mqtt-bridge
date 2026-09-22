/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link MappingRulesLoader}
 */
public class MappingRulesLoaderTest {

    /**
     * Test for loading mapping rules from a file.
     */
    @Test
    public void testLoadRules() throws Exception {
        String filePath = Objects.requireNonNull(getClass().getClassLoader().getResource("mapping-rules-regex.json")).getPath();

        List<MappingRule> rules = MappingRulesLoader.loadRules(filePath);

        assertThat("Should load 7 mapping rules",
                rules, notNullValue());
        assertThat("Should load 7 mapping rules",
                rules.size(), is(7));
        assertThat("Should not have null values",
                rules.stream().anyMatch(rule -> rule.getMqttTopicPattern() == null || rule.getKafkaTopicTemplate() == null), is(false));
    }

    /**
     * Test that loading mapping rules from a non-existent file throws.
     */
    @Test
    public void testLoadRulesFromMissingFileThrows() {
        assertThrows(java.io.IOException.class, () -> MappingRulesLoader.loadRules("/this/path/does-not-exist.json"));
    }
}
