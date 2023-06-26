/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.strimzi.kafka.bridge.mqtt.core.MqttKafkaMapper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Helper class to Load the rules from the configuration file
 */
public class MappingRulesLoader {

    private static final MappingRulesLoader INSTANCE = new MappingRulesLoader();
    // path of the topic mapping rule file
    private String mapperRuleFilePath;

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
     * Set the path of the mapper rule file
     *
     * @param mapperRuleFilePath the path of the mapper rule file
     */
    public void setMapperRuleFilePath(String mapperRuleFilePath) {
        this.mapperRuleFilePath = mapperRuleFilePath;
    }

    /**
     * Load the mapping rules from the file system and create the mapper instance.
     *
     * @see MqttKafkaMapper
     */
    public List<MappingRule> loadRules() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder json = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(this.mapperRuleFilePath));
        String line;
        // build the JSON string
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }
        // deserialize the JSON array to a list of MappingRule objects
        return mapper.readValue(json.toString(), mapper.getTypeFactory().constructCollectionType(List.class, MappingRule.class));
    }
}
