/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.kafka;

import io.strimzi.kafka.bridge.mqtt.config.KafkaConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Represents a Kafka producer for the Bridge.
 */
public class KafkaBridgeProducer {

    private final Producer<String, byte[]> noAckProducer;
    private final Producer<String, byte[]> ackOneProducer;

    /**
     * Constructor
     */
    public KafkaBridgeProducer(KafkaConfig config) {
        this.noAckProducer = createProducer(config, KafkaProducerAckLevel.ZERO);
        this.ackOneProducer = createProducer(config, KafkaProducerAckLevel.ONE);
    }

    /**
     * Send the given record to the Kafka topic
     *
     * @param record record to be sent
     * @return a future which completes when the record is acknowledged
     */
    public CompletionStage<RecordMetadata> send(ProducerRecord<String, byte[]> record) {
        CompletableFuture<RecordMetadata> promise = new CompletableFuture<>();

        this.ackOneProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                promise.completeExceptionally(exception);
            } else {
                promise.complete(metadata);
            }
        });
        return promise;
    }

    /**
     * Send the given record to the Kafka topic
     *
     * @param record record to be sent
     */
    public void sendNoAck(ProducerRecord<String, byte[]> record) {
        this.noAckProducer.send(record);
    }

    /**
     * Create the Kafka producer client with the given configuration
     */
    private Producer<String, byte[]> createProducer(KafkaConfig kafkaConfig, KafkaProducerAckLevel producerAckLevel) {
        Properties props = new Properties();
        props.putAll(kafkaConfig.getConfig());
        props.putAll(kafkaConfig.getProducerConfig().getConfig());
        props.put(ProducerConfig.ACKS_CONFIG, String.valueOf(producerAckLevel.getValue()));
        return new KafkaProducer<>(props, new StringSerializer(), new ByteArraySerializer());
    }

    /**
     * Close the producer
     */
    public void close() {

        if (this.noAckProducer != null) {
            this.noAckProducer.flush();
            this.noAckProducer.close();
        }

        if (this.ackOneProducer != null) {
            this.ackOneProducer.flush();
            this.ackOneProducer.close();
        }
    }
}
