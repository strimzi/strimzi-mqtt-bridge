/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents the bridge configuration properties
 *
 * @see KafkaConfig
 * @see MqttConfig
 */
public class BridgeConfig extends AbstractConfig {

    // Prefix for all the specific configuration parameters for the bridge
    public static final String BRIDGE_CONFIG_PREFIX = "bridge.";

    // Bridge identification number
    public static final String BRIDGE_ID = BRIDGE_CONFIG_PREFIX + "id";

    private final KafkaConfig kafkaConfig;
    private final MqttConfig mqttConfig;

    public BridgeConfig(Map<String, Object> config, KafkaConfig kafkaConfig, MqttConfig mqttConfig) {
        super(config);
        this.kafkaConfig = kafkaConfig;
        this.mqttConfig = mqttConfig;
    }

    public static BridgeConfig fromMap(Map<String, Object> map) {
        final KafkaConfig kafkaConfig = KafkaConfig.fromMap(map);
        final MqttConfig mqttConfig = MqttConfig.fromMap(map);
        return new BridgeConfig(map.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(BridgeConfig.BRIDGE_CONFIG_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), kafkaConfig, mqttConfig);
    }

    public KafkaConfig getKafkaConfig() {
        return this.kafkaConfig;
    }

    public MqttConfig getMqttConfig() {
        return this.mqttConfig;
    }

    public String getBridgeID() {
        return this.config.get(BridgeConfig.BRIDGE_ID) == null ? null : this.config.get(BridgeConfig.BRIDGE_ID).toString();
    }

    /**
     * @return the bridge configuration properties
     */
    @Override
    public String toString() {
        return "BridgeConfig(" +
                "config=" + this.config +
                ", kafkaConfig=" + this.kafkaConfig +
                ", mqttConfig=" + this.mqttConfig +
                ')';
    }
}
