/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.kafka;

import io.strimzi.kafka.bridge.mqtt.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating Kafka producers as needed.
 */
public class BridgeKafkaProducerFactory<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(BridgeKafkaProducerFactory.class);
    private static final BridgeKafkaProducerFactory INSTANCE = new BridgeKafkaProducerFactory<>();
    // kafka configuration
    private KafkaConfig kafkaConfig;
    private BridgeKafkaProducer<K, V> bridgeKafkaProducerZero;
    private BridgeKafkaProducer<K, V> bridgeKafkaProducerOne;


    /**
     * Constructor
     *
     */
    private BridgeKafkaProducerFactory() {}

    /**
     * Initialize the factory with the given configuration
     * @param kafkaConfig Kafka configuration
     */
    public void init(KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
    }

    /**
     * Get the singleton instance of the factory
     * @return BridgeKafkaProducerFactory
     */
    public static BridgeKafkaProducerFactory getInstance() {
        return INSTANCE;
    }


    /**
     * Get the Kafka producer for the given Mqtt QoS
     * @param qos Mqtt QoS
     * @return BridgeKafkaProducer
     */
    public BridgeKafkaProducer<K, V> getProducer(int qos) {
        if (qos == 0) {
            if (this.bridgeKafkaProducerZero == null) {
                this.bridgeKafkaProducerZero = new BridgeKafkaProducer<>(0);
                this.bridgeKafkaProducerZero.create(this.kafkaConfig);
            }
            return this.bridgeKafkaProducerZero;
        } else {
            if (this.bridgeKafkaProducerOne == null) {
                this.bridgeKafkaProducerOne = new BridgeKafkaProducer<>(1);
                this.bridgeKafkaProducerOne.create(this.kafkaConfig);
            }
            return this.bridgeKafkaProducerOne;
        }
    }
}
