/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import java.util.Map;
import java.util.stream.Collectors;

public class MqttConfig extends AbstractConfig {

    public static final String MQTT_CONFIG_PREFIX = "mqtt.";
    public static final String MQTT_HOST = MQTT_CONFIG_PREFIX + "host";

    public static final String MQTT_PORT = MQTT_CONFIG_PREFIX + "port";

    public static final String DEFAULT_MQTT_HOST = "localhost";

    public static final int DEFAULT_MQTT_PORT = 1883;

    public MqttConfig(Map<String, Object> config) {
        super(config);
    }

    public static MqttConfig fromMap(Map<String, Object> map) {
        return new MqttConfig(map.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(MqttConfig.MQTT_CONFIG_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public int getPort() {
        return Integer.parseInt(this.config.getOrDefault(MqttConfig.MQTT_PORT, MqttConfig.DEFAULT_MQTT_PORT).toString());
    }

    public String getHost() {
        return this.config.getOrDefault(MqttConfig.MQTT_HOST, MqttConfig.DEFAULT_MQTT_HOST).toString();
    }

    @Override
    public String toString(){
        return "MqttConfig(" +
                "config=" + config +
                ')';
    }

}
