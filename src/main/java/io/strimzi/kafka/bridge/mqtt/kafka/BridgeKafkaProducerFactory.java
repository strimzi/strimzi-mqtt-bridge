/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.kafka;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.strimzi.kafka.bridge.mqtt.config.KafkaConfig;
import io.strimzi.kafka.bridge.mqtt.utils.KafkaProducerAckLevel;

/**
 * Factory for creating Kafka producers as needed.
 */
public class BridgeKafkaProducerFactory {

    private static BridgeKafkaProducerFactory INSTANCE;
    private BridgeKafkaProducer bridgeKafkaProducerZero;
    private BridgeKafkaProducer bridgeKafkaProducerOne;
    private static boolean initialized = false;

    /**
     * Constructor
     */
    private BridgeKafkaProducerFactory() {
    }

    /**
     * Get the singleton instance of the factory
     * @return BridgeKafkaProducerFactory
     */
    public static synchronized BridgeKafkaProducerFactory getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BridgeKafkaProducerFactory();
        }
        return INSTANCE;
    }

    /**
     * Initialize the factory with the given configuration
     * @param kafkaConfig Kafka configuration
     */
    public void init(KafkaConfig kafkaConfig) {

        if (this.bridgeKafkaProducerZero == null) {
            this.bridgeKafkaProducerZero = new BridgeKafkaProducer(kafkaConfig, KafkaProducerAckLevel.ZERO);
        }

        if (this.bridgeKafkaProducerOne == null) {
            this.bridgeKafkaProducerOne = new BridgeKafkaProducer(kafkaConfig, KafkaProducerAckLevel.ONE);
        }
        initialized = true;
    }

    /**
     * Get the Kafka producer for the given Mqtt QoS
     *
     * @param qos Mqtt QoS
     * @return BridgeKafkaProducer
     */
    public BridgeKafkaProducer getProducer(MqttQoS qos) {

        if (!initialized) {
            throw new IllegalStateException("BridgeKafkaProducerFactory is not initialized");
        }

        return switch (qos) {
            case AT_MOST_ONCE -> this.bridgeKafkaProducerZero;
            case AT_LEAST_ONCE -> this.bridgeKafkaProducerOne;
            case EXACTLY_ONCE -> null;
            case FAILURE -> throw new IllegalStateException("Unexpected value: " + qos);
        };
    }
}
