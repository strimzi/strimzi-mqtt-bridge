/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.kafka;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.strimzi.kafka.bridge.mqtt.config.KafkaConfig;
import io.strimzi.kafka.bridge.mqtt.utils.KafkaProducerAckLevel;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.CompletionStage;

/**
 * A service to handle the Kafka Producers
 */
public class BridgeKafkaProducerService {
    // A Kafka Producer to handle mqtt messages with QoS 0. This producer has ack equals to 0
    private final BridgeKafkaProducer bridgeKafkaProducerWithNoAck;
    // A Kafka Producer to handle mqtt messages with QoS 1. This producer has ack equals to 1
    private final BridgeKafkaProducer bridgeKafkaProducerWithAckOne;

    /**
     * Constructor
     *
     * @param kafkaConfig Kafka configuration parameters
     */
    public BridgeKafkaProducerService(KafkaConfig kafkaConfig) {
        this.bridgeKafkaProducerWithNoAck = new BridgeKafkaProducer(kafkaConfig, KafkaProducerAckLevel.ZERO);
        this.bridgeKafkaProducerWithAckOne = new BridgeKafkaProducer(kafkaConfig, KafkaProducerAckLevel.ONE);
    }

    /**
     * Send the given record to the Kafka topic
     *
     * @param qos    MQTT QoS level
     * @param record record to be sent
     * @return a future which completes when the record is acknowledged
     */
    public CompletionStage<RecordMetadata> send(MqttQoS qos, ProducerRecord<String, byte[]> record) {
        return switch (qos) {
            case AT_MOST_ONCE -> bridgeKafkaProducerWithNoAck.send(record);
            case AT_LEAST_ONCE -> bridgeKafkaProducerWithAckOne.send(record);
            default -> throw new IllegalArgumentException("QoS level not supported");
        };
    }

    /**
     * Close the Kafka Producers
     */
    public void close() {
        bridgeKafkaProducerWithNoAck.close();
        bridgeKafkaProducerWithAckOne.close();

    }
}
