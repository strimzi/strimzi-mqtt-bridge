/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt;

import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.strimzi.kafka.bridge.mqtt.config.BridgeConfig;
import io.strimzi.kafka.bridge.mqtt.config.MqttConfig;
import io.strimzi.kafka.bridge.mqtt.config.MqttSslConfig;
import io.strimzi.kafka.bridge.mqtt.core.MqttServer;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRule;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRulesLoader;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for MQTT over TLS.
 */
@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "checkstyle:ClassFanOutComplexity"})
public class MqttSslIT {
    private static final String MQTT_SERVER_HOST = "127.0.0.1";
    private static final int MQTT_SERVER_PORT = 8883;
    private static final String MQTT_SSL_SERVER_URI = "ssl://" + MQTT_SERVER_HOST + ":" + MQTT_SERVER_PORT;
    private static final String MQTT_PLAIN_SERVER_URI = "tcp://" + MQTT_SERVER_HOST + ":" + MQTT_SERVER_PORT;

    private static MqttServer mqttBridge;

    @BeforeAll
    public static void beforeAll() throws IOException {
        Map<String, Object> config = new HashMap<>();
        config.put(BridgeConfig.BRIDGE_ID, "my-ssl-bridge");
        config.put(MqttConfig.MQTT_HOST, "0.0.0.0");
        config.put(MqttConfig.MQTT_PORT, MQTT_SERVER_PORT);
        config.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        config.put(MqttSslConfig.MQTT_SSL_CERTIFICATE_LOCATION, SslTestUtils.certificateLocation());
        config.put(MqttSslConfig.MQTT_SSL_KEY_LOCATION, SslTestUtils.keyLocation());
        config.put("kafka.bootstrap.servers", "localhost:9092");

        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        String mappingRulesPath = Objects.requireNonNull(MqttBridgetIT.class.getClassLoader().getResource("mapping-rules-regex.json")).getPath();
        List<MappingRule> mappingRules = MappingRulesLoader.loadRules(mappingRulesPath);

        mqttBridge = new MqttServer(BridgeConfig.fromMap(config), bossGroup, workerGroup, ChannelOption.SO_KEEPALIVE, mappingRules);
        mqttBridge.start();
    }

    @AfterAll
    public static void afterAll() {
        if (mqttBridge != null) {
            mqttBridge.stop();
        }
    }

    @Test
    public void testTlsConnection() throws Exception {
        MqttConnectOptions options = tlsConnectOptions();

        try (MqttClient client = new MqttClient(MQTT_SSL_SERVER_URI, getRandomMqttClientId(), null)) {
            IMqttToken conn = client.connectWithResult(options);

            assertThat("The session present flag should be false",
                    conn.getSessionPresent(), is(false));
            assertThat("The connection's response message type code should be MESSAGE_TYPE_CONNACK",
                    conn.getResponse().getType(), is(MqttWireMessage.MESSAGE_TYPE_CONNACK));
            assertThat("The client should be connected over TLS",
                    client.isConnected(), is(true));

            client.disconnect();
        }
    }

    @Test
    public void testMqttTrafficOverTls() throws Exception {
        MqttConnectOptions options = tlsConnectOptions();
        options.setKeepAliveInterval(1);

        try (MqttAsyncClient client = new MqttAsyncClient(MQTT_SSL_SERVER_URI, getRandomMqttClientId(), null)) {
            client.connect(options).waitForCompletion();
            Thread.sleep(3000); // Wait for a few seconds to ensure the keep-alive mechanism is working

            assertThat(client.isConnected(), is(true));
            client.disconnect();
        }
    }

    @Test
    public void testPlaintextConnectionToTlsPortFails() throws Exception {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(5);

        try (MqttClient client = new MqttClient(MQTT_PLAIN_SERVER_URI, getRandomMqttClientId(), null)) {
            assertThrows(MqttException.class, () -> client.connectWithResult(options));
            assertThat(client.isConnected(), is(false));
        }
    }

    private static MqttConnectOptions tlsConnectOptions() throws Exception {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setSocketFactory(SslTestUtils.trustServerCertificateSocketFactory());
        options.setHttpsHostnameVerificationEnabled(false);
        options.setConnectionTimeout(10);
        return options;
    }

    private String getRandomMqttClientId() {
        return "mqtt-ssl-client-" + UUID.randomUUID();
    }
}
