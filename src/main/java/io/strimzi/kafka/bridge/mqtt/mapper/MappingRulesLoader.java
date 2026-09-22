/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Helper class to Load the rules from the configuration file.
 */
public final class MappingRulesLoader {

    private MappingRulesLoader() {
    }

    /**
     * Load the mapping rules from the given file system path and return them.
     *
     * @param mapperRuleFilePath the path of the mapper rule file
     * @return the list of loaded mapping rules
     * @throws IOException if the rules file cannot be read or parsed
     * @see MqttKafkaMapper
     */
    public static List<MappingRule> loadRules(String mapperRuleFilePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // deserialize the JSON array to a list of MappingRule objects
        return mapper.readValue(Path.of(mapperRuleFilePath).toFile(), mapper.getTypeFactory().constructCollectionType(List.class, MappingRule.class));
    }
}
