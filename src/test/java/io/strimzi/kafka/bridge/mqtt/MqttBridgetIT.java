/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt;

import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.strimzi.kafka.bridge.mqtt.config.BridgeConfig;
import io.strimzi.kafka.bridge.mqtt.config.KafkaConfig;
import io.strimzi.kafka.bridge.mqtt.config.MqttConfig;
import io.strimzi.kafka.bridge.mqtt.core.MqttServer;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRulesLoader;
import io.strimzi.test.container.StrimziKafkaCluster;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;

import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for MQTT bridge
 */
@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "checkstyle:ClassFanOutComplexity"})
public class MqttBridgetIT {
    private static final Logger LOGGER = LogManager.getLogger(MqttBridgetIT.class);
    private static final String MQTT_SERVER_HOST = "0.0.0.0";
    private static final int MQTT_SERVER_PORT = 1883;
    private static final String MQTT_SERVER_URI = "tcp://" + MQTT_SERVER_HOST + ":" + MQTT_SERVER_PORT;
    private static final String KAFKA_TOPIC = "devices_bluetooth_data";
    private static KafkaConsumer<String, String> kafkaConsumerClient = null;
    private static MqttServer mqttBridge;
    private static StrimziKafkaCluster kafkaContainer;


    /**
     * Start the kafka Cluster
     * Start the MQTT bridge before all tests
     */
    @BeforeAll
    public static void beforeAll() {
        String kafkaBootstrapServers;
        try {
            kafkaContainer = new StrimziKafkaCluster.StrimziKafkaClusterBuilder()
                    .withNumberOfBrokers(1)
                    .build();
            kafkaContainer.start();
            kafkaBootstrapServers = kafkaContainer.getBootstrapServers();
        } catch (Exception e) {
            LOGGER.error("Exception occurred", e);
            throw e;
        }

        // instantiate the kafka consumer client
        kafkaConsumerClient = new KafkaConsumer<>(
                ImmutableMap.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG, "gid-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.name().toLowerCase(Locale.ROOT)
                ),
                new StringDeserializer(),
                new StringDeserializer()
        );

        // consumer client subscribes to the kafka topic
        kafkaConsumerClient.subscribe(Collections.singletonList(KAFKA_TOPIC));

        // prepare the configuration for the bridge
        BridgeConfig bridgeConfig = BridgeConfig.fromMap(
                ImmutableMap.of(
                        BridgeConfig.BRIDGE_ID, "my-bridge",
                        KafkaConfig.KAFKA_CONFIG_PREFIX + ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
                        MqttConfig.MQTT_HOST, MQTT_SERVER_HOST,
                        MqttConfig.MQTT_PORT, MQTT_SERVER_PORT
                ));

        // prepare the mapping rules
        String mappingRulesPath = Objects.requireNonNull(MqttBridgetIT.class.getClassLoader().getResource("mapping-rules-regex.json")).getPath();
        MappingRulesLoader.getInstance().init(mappingRulesPath);

        // start the MQTT bridge
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        mqttBridge = new MqttServer(bridgeConfig, bossGroup, workerGroup, ChannelOption.SO_KEEPALIVE);
        mqttBridge.start();
    }

    /**
     * Test the client's connection to the Bridge
     */
    @Test
    public void testConnection() throws MqttException {
        try (MqttClient client = new MqttClient(MQTT_SERVER_URI, getRandomMqttClientId(), null)) {
            // Set up options for the connection
            MqttConnectOptions options = new MqttConnectOptions();

            IMqttToken conn = client.connectWithResult(options);

            assertThat("The session present flag should be false",
                    conn.getSessionPresent(), is(false));

            assertThat("The connection's response message type code should be MESSAGE_TYPE_CONNACK",
                    conn.getResponse().getType(), is(MqttWireMessage.MESSAGE_TYPE_CONNACK));

            assertThat("The client should be connected",
                    client.isConnected(), is(true));

            // Disconnect the client
            client.disconnect();
        }
    }

    /**
     * Test concurrent connections to the Bridge
     */
    @Test
    public void testConcurrentConnections() throws MqttException {
        List<MqttClient> clients = new ArrayList<>();

        // Create 10 clients
        for (int i = 0; i < 10; i++) {
            clients.add(new MqttClient(MQTT_SERVER_URI, getRandomMqttClientId(), null));
        }
        MqttConnectOptions options = new MqttConnectOptions();

        // Connect all clients and test the connection
        clients.stream().unordered().parallel().forEach(client -> {
            try {
                IMqttToken conn = client.connectWithResult(options);
                assertThat("The session present flag should be false",
                        conn.getSessionPresent(), is(false));
                assertThat("The connection's response message type code should be MESSAGE_TYPE_CONNACK",
                        conn.getResponse().getType(), is(MqttWireMessage.MESSAGE_TYPE_CONNACK));
                assertThat("The client should be connected",
                        client.isConnected(), is(true));
            } catch (MqttException e) {
                LOGGER.error("Exception occurred", e);
            } finally {
                try {
                    client.disconnect();
                } catch (MqttException e) {
                    LOGGER.error("Exception occurred", e);
                }
            }
        });
    }

    /**
     * Test the ability to send and receive a pingreq
     */
    @Test
    public void testPing() throws MqttException {
        try (MqttAsyncClient client = new MqttAsyncClient(MQTT_SERVER_URI, getRandomMqttClientId(), null)) {
            // Set up options for the connection
            MqttConnectOptions options = new MqttConnectOptions();
            options.setKeepAliveInterval(1);

            client.connect(options).waitForCompletion();
            
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // nop
            }

            assertThat("The client still is connected to the server", client.isConnected(), is(true));

            client.disconnect();
        }
    }

    /**
     * Test the client publishing a message to the bridge, and the bridge maps and produce the message to the kafka topic
     * The kafka consumer client consumes the message from the kafka topic.
     */
    @Test
    public void testPublishAndConsumer() throws MqttException {
        String mqttTopic = "devices/bluetooth/type/audio/data";
        String kafkaKey = "devices_audio";

        try (MqttAsyncClient client = new MqttAsyncClient(MQTT_SERVER_URI, getRandomMqttClientId(), null)) {
            MqttConnectOptions options = new MqttConnectOptions();

            client.connect(options).waitForCompletion();

            client.publish(mqttTopic, "Hello world".getBytes(), MqttQoS.AT_MOST_ONCE.value(), false).waitForCompletion();

            ConsumerRecords<String, String> records = kafkaConsumerClient.poll(Duration.ofSeconds(20));

            assertThat("The record should be present", records.count(), is(1));

            ConsumerRecord<String, String> record = records.iterator().next();

            assertThat("The key should be " + kafkaKey,
                    record.key(), is(kafkaKey));
            assertThat("The value should be Hello world",
                    record.value(), is("Hello world"));
            assertThat("The topic should be " + KAFKA_TOPIC,
                    record.topic(), is(KAFKA_TOPIC));
            assertThat("The record headers should contain the MQTT topic " + mqttTopic,
                    record.headers().lastHeader("mqtt-topic").value(), is(mqttTopic.getBytes()));

            // Disconnect the client
            client.disconnect();
        }
    }

    /**
     * Close the kafka consumer client, stop the Bridge and stop the kafka cluster
     */
    @AfterAll
    public static void afterAll() {
        kafkaConsumerClient.close();
        mqttBridge.stop();
        kafkaContainer.stop();
    }

    /**
     * Randomly generate a new client id before each test
     */
    private String getRandomMqttClientId() {
        return "mqtt-client-" + new Random().nextInt(20);
    }
}
