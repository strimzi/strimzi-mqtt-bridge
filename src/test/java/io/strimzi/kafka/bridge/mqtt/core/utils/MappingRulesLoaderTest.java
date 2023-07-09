/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRule;
import io.strimzi.kafka.bridge.mqtt.utils.MappingRulesLoader;
import org.junit.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doNothing;

/**
 * Unit tests for {@link MappingRulesLoader}
 */
public class MappingRulesLoaderTest {

    /**
     * Test for loading mapping rules from a file.
     */
    @Test
    public void testLoadRules() throws Exception {
        String filePath = Objects.requireNonNull(getClass().getClassLoader().getResource("mapping-rules.json")).getPath();
        MappingRulesLoader loader = mock(MappingRulesLoader.class);

        // 1st loadRules call throws exception. 2nd loadRules call returns the rules
        when(loader.loadRules())
                .thenThrow(new IllegalStateException("MappingRulesLoader is not initialized"))
                .thenAnswer(invocationOnMock -> {
                    ObjectMapper mapper = new ObjectMapper();
                    // deserialize the JSON array to a list of MappingRule objects
                    return mapper.readValue(Path.of(filePath).toFile(), mapper.getTypeFactory().constructCollectionType(List.class, MappingRule.class));
                });

        // the mapping rules loader is not initialized
        Exception exception = assertThrows(IllegalStateException.class, loader::loadRules);
        String expectedMessage = "MappingRulesLoader is not initialized";

        assertThat("Should throw an exception",
                exception, notNullValue());
        assertThat("Should throw an exception with the correct message",
                exception.getMessage(), is(expectedMessage));

        // init the mapping rules loader
        loader.init(filePath);
        List<MappingRule> rules = loader.loadRules();

        assertThat("Should load 7 mapping rules",
                rules.size(), is(7));
        assertThat("Should not have null values",
                rules.stream().anyMatch(rule -> rule.getMqttTopicPattern() == null || rule.getKafkaTopicTemplate() == null), is(false));

        verify(loader, atMostOnce()).init(filePath);
    }

    /**
     * Test for initializing mapping rules loader more than once.
     */
    @Test
    public void testInitMoreThanOnce() {
        String filePath = Objects.requireNonNull(getClass().getClassLoader().getResource("mapping-rules.json")).getPath();
        MappingRulesLoader loader = mock(MappingRulesLoader.class);

        // 1st init call, do Nothing. 2nd init call throw exception
        doNothing().doThrow(new IllegalStateException("MappingRulesLoader is already initialized")).when(loader).init(filePath);

        // first init
        loader.init(filePath);

        // prepare exception, try to init again
        Exception exception =  assertThrows(IllegalStateException.class,  () -> loader.init(filePath));
        String expectedMessage = "MappingRulesLoader is already initialized";

        assertThat("Should throw an exception",
                exception, notNullValue());
        assertThat("Should throw an illegal state exception",
                exception.getMessage(), is(expectedMessage));

        verify(loader, times(2)).init(filePath);
    }
}
