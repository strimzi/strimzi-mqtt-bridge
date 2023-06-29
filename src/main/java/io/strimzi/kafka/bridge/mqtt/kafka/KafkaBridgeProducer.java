package io.strimzi.kafka.bridge.mqtt.kafka;


import io.strimzi.kafka.bridge.mqtt.config.KafkaConfig;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Represents a Kafka producer client.
 * @param <K> key
 * @param <V> value
 */
public class KafkaBridgeProducer<K, V> {

    private static final KafkaBridgeProducer<Object, Object> INSTANCE = new KafkaBridgeProducer<>();
    private static final Logger logger = LoggerFactory.getLogger(KafkaBridgeProducer.class);
    private KafkaConfig kafkaConfig;
    private Serializer<K> keySerializer;
    private Serializer<V> valueSerializer;
    private Producer<K, V> kafkaProducerClient;

    /**
     * Get the private Kafka producer client instance
     *
     * @return Kafka producer client instance
     */
    private KafkaBridgeProducer() {
    }

    /**
     * init the Kafka producer client
     *
     * @param kafkaConfig     Kafka configuration
     //* @param keySerializer   Kafka key serializer
     //* @param valueSerializer Kafka value serializer
     */
    public void init(KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
        //this.keySerializer = keySerializer;
        //this.valueSerializer = valueSerializer;
    }

    /**
     * Create the Kafka producer client
     */
    public static KafkaBridgeProducer getInstance() {
        return INSTANCE;
    }
    
    /**
     * Create the Kafka producer client
     *
     */
    public void createProducer() {
        Properties props = new Properties();
        logger.info(this.kafkaConfig.getConfig().toString());
        props.putAll(kafkaConfig.getConfig());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        this.kafkaProducerClient = new KafkaProducer<>(props);
    }

    /**
     * Send a record to Kafka topic
     */
    public CompletionStage<RecordMetadata> send(String KafkaTopic, ProducerRecord<K, V> record) {
        logger.info("Sending message to Kafka topic {}", KafkaTopic);
        CompletionStage<RecordMetadata> resultPromise = new CompletableFuture<>();
        this.kafkaProducerClient.send(record , (metadata, exception) -> {
            if (exception != null) {
                logger.error("Error sending message to Kafka topic {}: {}", KafkaTopic, exception.getMessage());
                resultPromise.toCompletableFuture().completeExceptionally(exception);
            } else {
                logger.info("Message sent to Kafka topic {} at offset {}", KafkaTopic, metadata);
                resultPromise.toCompletableFuture().complete(metadata);
            }
        });
        return resultPromise;
    }
}
