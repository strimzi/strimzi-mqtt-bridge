package io.strimzi.kafka.bridge.mqtt.core.utils;

import io.strimzi.kafka.bridge.mqtt.utils.MappingRule;
import io.strimzi.kafka.bridge.mqtt.utils.MappingRulesLoader;
import org.junit.Test;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        loader.setMapperRuleFilePath(filePath);
        List<MappingRule> rules = loader.loadRules();

        assertThat("Should load 7 mapping rules",
                rules.size(), is(7));

        assertThat("Should not have null values",
                rules.stream().anyMatch(rule -> rule.getMqttTopicPattern() == null || rule.getKafkaTopicTemplate() == null), is(false));
    }
}
