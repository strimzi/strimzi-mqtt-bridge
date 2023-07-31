/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.clients;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * A Kafka consumer client
 */
public class KafkaConsumerClient {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerClient.class);
    static final String CONSUMER_GROUP_ID = "any-consumer-group";
    private final KafkaConsumer<String, String> consumer;

    /**
     * Constructor
     *
     * @param bootstrapServers bootstrap servers
     */
    public KafkaConsumerClient(String bootstrapServers) {
        // Create consumer properties
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(properties);
    }

    /**
     * Subscribe to a topic
     *
     * @param topic topic to subscribe to
     */
    public void subscribe(String topic) {
        consumer.subscribe(Collections.singletonList(topic));
        logger.info("Subscribed to topic: {}", topic);
    }

    /**
     * Poll for records
     *
     * @param duration duration to poll for
     * @return records
     */
    public ConsumerRecords<String, String> poll(Duration duration) {
        logger.info("Polling for records");
        return consumer.poll(duration);
    }

    /**
     * Close the consumer
     */
    public void close() {
        consumer.close();
    }
}
