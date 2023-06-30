package io.strimzi.kafka.bridge.mqtt.core.utils;

import io.strimzi.kafka.bridge.mqtt.mapper.MappingRule;
import io.strimzi.kafka.bridge.mqtt.utils.MappingRulesLoader;
import org.junit.Test;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

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
        MappingRulesLoader loader = MappingRulesLoader.getInstance();

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
        loader.close();
    }

    /**
     * Test for initializing mapping rules loader more than once.
     */
    @Test
    public void testInitMoreThanOnce() {
        String filePath = Objects.requireNonNull(getClass().getClassLoader().getResource("mapping-rules.json")).getPath();
        MappingRulesLoader loader = MappingRulesLoader.getInstance();

        // first init
        loader.init(filePath);

        // prepare exception, try to init again
        Exception exception =  assertThrows(IllegalStateException.class,  () -> loader.init(filePath));

        String expectedMessage = "MappingRulesLoader is already initialized";

        assertThat("Should throw an exception",
                exception, notNullValue());
        assertThat("Should throw an illegal state exception",
                exception.getMessage(), is(expectedMessage));
        loader.close();
    }
}
