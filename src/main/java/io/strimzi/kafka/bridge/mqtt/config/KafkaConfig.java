/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents configurations related to Kafka
 * @see AbstractConfig
 */
public class KafkaConfig extends AbstractConfig {

    // Prefix for all the specific configuration parameters for Kafka in the properties file
    public static final String KAFKA_CONFIG_PREFIX = "kafka.";

    /** Sensitive Kafka config keys, mirroring {@code ConfigDef.Type.PASSWORD} declarations in Kafka's SslConfigs and SaslConfigs */
    static final Set<String> KAFKA_SENSITIVE_CONFIG_KEYS = Set.of(
            "ssl.keystore.password",
            "ssl.key.password",
            "ssl.truststore.password",
            "ssl.keystore.key",
            "ssl.keystore.certificate.chain",
            "ssl.truststore.certificates",
            "sasl.jaas.config",
            "sasl.oauthbearer.client.credentials.client.secret",
            "sasl.oauthbearer.assertion.private.key.passphrase"
    );

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
        return "KafkaConfig(" +
                "config=" + this.hideSecrets(KAFKA_SENSITIVE_CONFIG_KEYS) +
                ", kafkaProducerConfig=" + kafkaProducerConfig +
                ")";
    }
}
