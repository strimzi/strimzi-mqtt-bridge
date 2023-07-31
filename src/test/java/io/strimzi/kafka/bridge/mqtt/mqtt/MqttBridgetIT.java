/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.mqtt;

import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.strimzi.kafka.bridge.mqtt.clients.KafkaConsumerClient;
import io.strimzi.kafka.bridge.mqtt.config.BridgeConfig;
import io.strimzi.kafka.bridge.mqtt.config.KafkaConfig;
import io.strimzi.kafka.bridge.mqtt.core.MqttServer;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRulesLoader;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

/**
 * Integration test for MQTT bridge
 */
public class MqttBridgetIT {

    private static final Logger logger = LoggerFactory.getLogger(MqttBridgetIT.class);
    private static final String BROKER_URI = "tcp://localhost:1883";
    private static final String MQTT_TOPIC = "devices/bluetooth/type/audio/data";
    private static final String KAFKA_TOPIC = "devices_bluetooth_data";
    private static final String KAFKA_KEY = "devices_audio";
    private static String CLIENT_ID = "myClientId";
    private static MqttServer mqttBridge;
    private static Thread prepareServerThread;
    private static KafkaConsumerClient kafkaConsumerClient;


    /**
     * Start the kafka Cluster
     * Start the MQTT bridge before all tests
     */
    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        final Map<String, Object> config = new HashMap<>();
        String kafkaBootstrapServers = "localhost:9092";
        try (StrimziKafkaContainer container = new StrimziKafkaContainer()) {
            container.start();
            kafkaBootstrapServers = container.getBootstrapServers();
        } catch (Exception e) {
            logger.error("Error while starting the kafka cluster", e);
        }

        // prepare the configuration for the bridge
        config.put(KafkaConfig.KAFKA_CONFIG_PREFIX + ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        config.put(BridgeConfig.BRIDGE_ID, "my-bridge");

        // instantiate the kafka consumer client
        kafkaConsumerClient = new KafkaConsumerClient(kafkaBootstrapServers);

        // consumer client subscribes to the kafka topic
        kafkaConsumerClient.subscribe(KAFKA_TOPIC);

        // start the bridge server in a separate thread
        prepareServerThread = new Thread(() -> {
            try {
                EventLoopGroup bossGroup = new NioEventLoopGroup();
                EventLoopGroup workerGroup = new NioEventLoopGroup();

                BridgeConfig bridgeConfig = BridgeConfig.fromMap(config);
                String mappingRulesPath = Objects.requireNonNull(MqttBridgetIT.class.getClassLoader().getResource("mapping-rules-regex.json")).getPath();
                MappingRulesLoader.getInstance().init(mappingRulesPath);

                mqttBridge = new MqttServer(bridgeConfig, bossGroup, workerGroup, ChannelOption.SO_KEEPALIVE);
                mqttBridge.start();
            } catch (InterruptedException interruptedException) {
                // in this case, we are interrupting the thread, so we don't need to do anything
                logger.info("Shutting down the server");
            } catch (Exception e) {
                logger.error("Exception occurred", e);
            }
        });
        prepareServerThread.start();
        Thread.sleep(5000);
    }

    /**
     * Create a new client id for each test randomly after each test
     */
    @AfterEach
    public void afterEach() {
        CLIENT_ID = "myClientId" + new Random().nextInt(1000);
    }

    /**
     * Test the client's connection to the Bridge
     */
    @Test
    public void testConnection() throws MqttException {
        try (MqttClient client = new MqttClient(BROKER_URI, CLIENT_ID)) {
            // Set up options for the connection
            MqttConnectOptions options = new MqttConnectOptions();

            IMqttToken conn = client.connectWithResult(options);

            assertThat("The session present flag should be false",
                    conn.getSessionPresent(), is(false));

            assertThat("The client should be connected",
                    client.isConnected(), is(true));

            // Disconnect the client
            client.disconnect();
        }
    }

    /**
     * Test the client publish a message to the bridge, and the bridge forwards the message to the kafka topic
     * The kafka consumer client consumes the message from the kafka topic.
     *
     * @throws MqttException
     */
    @Test
    public void testPublishAndConsumer() throws MqttException {
        try (MqttClient client = new MqttClient(BROKER_URI, CLIENT_ID)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(false);
            options.setConnectionTimeout(10);

            client.connect(options);

            client.publish(MQTT_TOPIC, "Hello world".getBytes(), MqttQoS.AT_MOST_ONCE.value(), false);

            ConsumerRecords<String, String> records = kafkaConsumerClient.poll(Duration.ofSeconds(20));
            // this is not polling the records accordingly
            assertThat("The record should be present", records.count(), is(1));

            assertThat("The key should be " + KAFKA_KEY,
                    records.iterator().next().key(), is(KAFKA_KEY));
            assertThat("The value should be Hello world",
                    records.iterator().next().value(), is("Hello world"));
            assertThat("The topic should be " + KAFKA_TOPIC,
                    records.iterator().next().topic(), is("KAFKA_TOPIC"));

            client.disconnect();
        }
    }

    /**
     * Stop the bridge server and close the kafka consumer client after all tests
     *
     * @throws InterruptedException
     */
    @AfterAll
    public static void afterAll() throws InterruptedException {
        Thread.sleep(5000);
        prepareServerThread.interrupt();
        while (prepareServerThread.isAlive()) {
            Thread.sleep(1000);
        }
        kafkaConsumerClient.close();
    }
}
