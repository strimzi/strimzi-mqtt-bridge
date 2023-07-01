/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRule;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Helper class to Load the rules from the configuration file
 */
public class MappingRulesLoader {

    private static final MappingRulesLoader INSTANCE = new MappingRulesLoader();
    // path of the topic mapping rule file
    private String mapperRuleFilePath;
    private boolean initialized = false;

    /**
     * Initialize the MappingRulesLoader
     *
     * @param mapperRuleFilePath the path of the mapper rule file
     */
    public void init(String mapperRuleFilePath) {
        if (!initialized) {
            this.mapperRuleFilePath = mapperRuleFilePath;
            initialized = true;
        } else {
            throw new IllegalStateException("MappingRulesLoader is already initialized");
        }
    }

    /**
     * Private constructor
     */
    private MappingRulesLoader() {
    }

    /**
     * Get the singleton instance of the MappingRulesLoader
     *
     * @return the singleton instance of the MappingRulesLoader
     */
    public static MappingRulesLoader getInstance() {
        return INSTANCE;
    }

    /**
     * Load the mapping rules from the file system and create the mapper instance.
     *
     * @see MqttKafkaMapper
     */
    public List<MappingRule> loadRules() throws IOException {

        if (!initialized) {
            throw new IllegalStateException("MappingRulesLoader is not initialized");
        }

        ObjectMapper mapper = new ObjectMapper();

        // deserialize the JSON array to a list of MappingRule objects
        return mapper.readValue(Path.of(this.mapperRuleFilePath).toFile(), mapper.getTypeFactory().constructCollectionType(List.class, MappingRule.class));
    }
}
