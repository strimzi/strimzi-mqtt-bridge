/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents configurations related to MQTT
 * @see AbstractConfig
 */
public class MqttConfig extends AbstractConfig {

    // Prefix for all the specific configuration parameters for MQTT in the properties file
    public static final String MQTT_CONFIG_PREFIX = "mqtt.";

    public static final String MQTT_HOST = MQTT_CONFIG_PREFIX + "host";

    public static final String MQTT_PORT = MQTT_CONFIG_PREFIX + "port";

    public static final String DEFAULT_MQTT_HOST = "0.0.0.0";

    public static final int DEFAULT_MQTT_PORT = 1883;

    /**
     * Constructor
     *
     * @param config configuration parameters map
     */
    public MqttConfig(Map<String, Object> config) {
        super(config);
    }

    /**
     * Build a MQTT configuration object from a map of configuration parameters
     *
     * @param map configuration parameters map
     * @return a new instance of MqttConfig
     */
    public static MqttConfig fromMap(Map<String, Object> map) {
        return new MqttConfig(map.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(MqttConfig.MQTT_CONFIG_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * @return the MQTT server port
     */
    public int getPort() {
        return Integer.parseInt(this.config.getOrDefault(MqttConfig.MQTT_PORT, MqttConfig.DEFAULT_MQTT_PORT).toString());
    }

    /**
     * @return the MQTT server host
     */
    public String getHost() {
        return this.config.getOrDefault(MqttConfig.MQTT_HOST, MqttConfig.DEFAULT_MQTT_HOST).toString();
    }

    @Override
    public String toString() {
        return "MqttConfig(" +
                "config=" + config +
                ")";
    }

}
