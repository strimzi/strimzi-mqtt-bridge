/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.strimzi.kafka.bridge.mqtt.config.BridgeConfig;
import io.strimzi.kafka.bridge.mqtt.config.MqttConfig;
import io.strimzi.kafka.bridge.mqtt.config.MqttSslConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for MQTT server SSL startup failures.
 */
public class MqttServerSslTest {

    @Test
    public void testConstructorFailsWhenSslEnabledWithoutCertificateAndKey() {
        Map<String, Object> config = new HashMap<>();
        config.put(MqttConfig.MQTT_HOST, "0.0.0.0");
        config.put(MqttConfig.MQTT_PORT, 0);
        config.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        config.put("kafka.bootstrap.servers", "localhost:9092");

        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            assertThrows(IllegalArgumentException.class, () ->
                    new MqttServer(BridgeConfig.fromMap(config), bossGroup, workerGroup, ChannelOption.SO_KEEPALIVE, null));
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Test
    public void testConstructorFailsWhenCertificateFileIsMissing() {
        Map<String, Object> config = new HashMap<>();
        config.put(MqttConfig.MQTT_HOST, "0.0.0.0");
        config.put(MqttConfig.MQTT_PORT, 0);
        config.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        config.put(MqttSslConfig.MQTT_SSL_CERTIFICATE_LOCATION, "/tmp/missing-server.crt");
        config.put(MqttSslConfig.MQTT_SSL_KEY_LOCATION, "/tmp/missing-server.key");
        config.put("kafka.bootstrap.servers", "localhost:9092");

        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            assertThrows(IllegalArgumentException.class, () ->
                    new MqttServer(BridgeConfig.fromMap(config), bossGroup, workerGroup, ChannelOption.SO_KEEPALIVE, null));
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
