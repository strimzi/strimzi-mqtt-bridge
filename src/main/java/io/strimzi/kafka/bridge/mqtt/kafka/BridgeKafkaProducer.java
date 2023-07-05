/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.kafka;

import io.strimzi.kafka.bridge.mqtt.config.KafkaConfig;
import io.strimzi.kafka.bridge.mqtt.utils.KafkaProducerAckLevel;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Represents a Kafka producer for the Bridge.
 */
public class BridgeKafkaProducer<K, V> {

    private final KafkaProducerAckLevel ackLevel;
    private Producer<K, V> clientProducer;

    /**
     * Constructor
     */
    public BridgeKafkaProducer(KafkaProducerAckLevel ackLevel) {
        this.ackLevel = ackLevel;
    }

    /**
     * Send the given record to the Kafka topic
     * @param record record to be sent
     * @return a future which completes when the record is acknowledged
     */
    public CompletionStage<RecordMetadata> send(ProducerRecord<K, V> record) {
        CompletableFuture<RecordMetadata> promise = new CompletableFuture<>();

        this.clientProducer.send(record, (metadata, exception) -> {
            if (exception == null) {
                promise.complete(metadata);
            } else {
                promise.completeExceptionally(exception);
            }
        });
        return promise;
    }
    /**
     * Create the Kafka producer client with the given configuration
     */
    public void create(KafkaConfig kafkaConfig) {
        Properties props = new Properties();
        String keySerializer = StringSerializer.class.getName();
        String valueSerializer = StringSerializer.class.getName();
        props.putAll(kafkaConfig.getConfig());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        props.put(ProducerConfig.ACKS_CONFIG, this.ackLevel.getValue());
        this.clientProducer = new KafkaProducer<>(props);
    }

    /**
     * Close the producer
     */
    public void close() {
        if (this.clientProducer != null) {
            this.clientProducer.close();
        }
    }
}
