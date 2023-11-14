/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import io.strimzi.kafka.bridge.mqtt.mapper.MqttKafkaMapper;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents the bridge configuration properties
 *
 * @see MqttConfig
 * @see KafkaConfig
 */
public class BridgeConfig extends AbstractConfig {

    // Prefix for all the specific configuration parameters for the bridge
    public static final String BRIDGE_CONFIG_PREFIX = "bridge.";

    // Bridge identification number
    public static final String BRIDGE_ID = BRIDGE_CONFIG_PREFIX + "id";

    // Bridge default topic name
    public static final String BRIDGE_DEFAULT_TOPIC = BRIDGE_CONFIG_PREFIX + "topic.default";

    private final MqttConfig mqttConfig;
    private final KafkaConfig kafkaConfig;

    /**
     * Constructor
     *
     * @param config      configuration parameters map
     * @param mqttConfig  MQTT configuration properties
     * @param kafkaConfig Kafka configuration properties
     */
    public BridgeConfig(Map<String, Object> config, MqttConfig mqttConfig, KafkaConfig kafkaConfig) {
        super(config);
        this.mqttConfig = mqttConfig;
        this.kafkaConfig = kafkaConfig;
    }

    /**
     * Build a bridge configuration object from a map of configuration parameters
     *
     * @param map configuration parameters map
     * @return a new instance of BridgeConfig
     */
    public static BridgeConfig fromMap(Map<String, Object> map) {
        final MqttConfig mqttConfig = MqttConfig.fromMap(map);
        final KafkaConfig kafkaConfig = KafkaConfig.fromMap(map);
        return new BridgeConfig(map.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(BridgeConfig.BRIDGE_CONFIG_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), mqttConfig, kafkaConfig);
    }

    /**
     * @return the Kafka configuration properties
     */
    public KafkaConfig getKafkaConfig() {
        return this.kafkaConfig;
    }

    /**
     * @return the MQTT configuration properties
     */
    public MqttConfig getMqttConfig() {
        return this.mqttConfig;
    }

    /**
     * @return the bridge identification number
     */
    public String getBridgeID() {
        return this.config.get(BridgeConfig.BRIDGE_ID) == null ? null : this.config.get(BridgeConfig.BRIDGE_ID).toString();
    }

    /**
     * @return the bridge default topic name
     * If not set, the default topic name is "messages_default"
     */
    public String getBridgeDefaultTopic() {
        return this.config.get(BridgeConfig.BRIDGE_DEFAULT_TOPIC) == null ? MqttKafkaMapper.MAPPER_DEFAULT_KAFKA_TOPIC : this.config.get(BridgeConfig.BRIDGE_DEFAULT_TOPIC).toString();
    }

    /**
     * @return the bridge configuration properties
     */
    @Override
    public String toString() {
        return "BridgeConfig(" +
                "config=" + this.config +
                ", mqttConfig=" + this.mqttConfig +
                ", kafkaConfig=" + this.kafkaConfig +
                ")";
    }
}
