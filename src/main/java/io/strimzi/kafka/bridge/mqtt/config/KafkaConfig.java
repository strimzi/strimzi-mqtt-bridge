/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

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
    private final KafkaAdminConfig kafkaAdminConfig;

    /**
     * Constructor
     *
     * @param config configuration parameters map
     * @param kafkaProducerConfig Kafka producer configuration properties
     */
    public KafkaConfig(Map<String, Object> config, KafkaProducerConfig kafkaProducerConfig, KafkaAdminConfig kafkaAdminConfig) {
        super(config);
        this.kafkaProducerConfig = kafkaProducerConfig;
        this.kafkaAdminConfig = kafkaAdminConfig;
    }

    /**
     * Build a Kafka configuration object from a map of configuration parameters
     *
     * @param config configuration parameters map
     * @return a new instance of KafkaConfig
     */
    public static KafkaConfig fromMap(Map<String, Object> config) {
        final KafkaProducerConfig kafkaProducerConfig = KafkaProducerConfig.fromMap(config);
        final KafkaAdminConfig kafkaAdminConfig = KafkaAdminConfig.fromMap(config);
        return new KafkaConfig(config.entrySet().stream()
                .filter((entry-> entry
                .getKey().startsWith(KafkaConfig.KAFKA_CONFIG_PREFIX)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), kafkaProducerConfig, kafkaAdminConfig);
    }

    /**
     * @return the Kafka producer configuration properties
     */
    public KafkaProducerConfig getKafkaProducerConfig() {
        return kafkaProducerConfig;
    }

    /**
     * @return the Kafka admin configuration properties
     */
    public KafkaAdminConfig getKafkaAdminConfig() {
        return kafkaAdminConfig;
    }


    @Override
    public String toString() {
        return "KafkaConfig(" +
                "config=" + config +
                ", kafkaProducerConfig=" + kafkaProducerConfig +
                ')';
    }
}
