/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents configurations related to Kafka
 * @see AbstractConfig
 */
public class KafkaConfig extends AbstractConfig {

    // Prefix for all the specific configuration parameters for Kafka in the properties file
    public static final String KAFKA_CONFIG_PREFIX = "kafka.";

    // Kafka bootstrap server configuration. This will be removed when we add the apache kafka client
    public static final String BOOTSTRAP_SERVERS_CONFIG = KAFKA_CONFIG_PREFIX + "bootstrap.servers";
    private final KafkaProducerConfig kafkaProducerConfig;

    /**
     * Constructor
     *
     * @param config configuration parameters map
     * @param kafkaProducerConfig Kafka producer configuration properties
     */
    public KafkaConfig(Map<String, Object> config, KafkaProducerConfig kafkaProducerConfig) {
        super(config);
        this.kafkaProducerConfig = kafkaProducerConfig;
    }

    /**
     * Build a Kafka configuration object from a map of configuration parameters
     *
     * @param config configuration parameters map
     * @return a new instance of KafkaConfig
     */
    public static KafkaConfig fromMap(Map<String, Object> config) {
        final KafkaProducerConfig kafkaProducerConfig = KafkaProducerConfig.fromMap(config);
        return new KafkaConfig(config.entrySet().stream()
                .filter((entry -> entry.getKey().startsWith(KafkaConfig.KAFKA_CONFIG_PREFIX) &&
                        !entry.getKey().startsWith(KafkaProducerConfig.KAFKA_PRODUCER_CONFIG_PREFIX)))
                .collect(Collectors.toMap((e) -> e.getKey().substring(KAFKA_CONFIG_PREFIX.length()), Map.Entry::getValue)), kafkaProducerConfig);
    }

    /**
     * @return the Kafka producer configuration properties
     */
    public KafkaProducerConfig getProducerConfig() {
        return kafkaProducerConfig;
    }

    @Override
    public String toString() {
        Map<String, Object> configToString = this.hidePasswords();
        return "KafkaConfig(" +
                "config=" + configToString +
                ", kafkaProducerConfig=" + kafkaProducerConfig +
                ")";
    }

    /**
     * Hides Kafka related password(s) configuration (i.e. truststore and keystore)
     * by replacing each actual password with [hidden] string
     *
     * @return updated configuration with hidden password(s)
     */
    private Map<String, Object> hidePasswords() {
        Map<String, Object> configToString = new HashMap<>(this.config);
        configToString.entrySet().stream()
                .filter(e -> e.getKey().contains("password"))
                .forEach(e -> e.setValue("[hidden]"));
        return configToString;
    }
}
