/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.utils;

/**
 * Represents the Kafka producer ack level
 */
public enum KafkaProducerAckLevel {
    // This ack level is used when the kafka producer is expecting no acks
    ZERO(0),
    // This ack level is used when the kafka producer is expecting acks from the leader
    ONE(1),
    // This ack level is used when the kafka producer is expecting acks from all the replicas
    ALL(-1);

    private final int value;

    /**
     * Constructor
     *
     * @param value the value of the ack level
     */
    KafkaProducerAckLevel(int value) {
        this.value = value;
    }

    /**
     * Get the ack level from the value
     *
     * @param value the value of the ack level
     * @return the ack level
     */
    public static KafkaProducerAckLevel valueOf(int value) {
        return switch (value) {
            case 0 -> ZERO;
            case 1 -> ONE;
            case -1 -> ALL;
            default -> throw new IllegalArgumentException("Unknown KafkaProducerAckLevel value: " + value);
        };
    }

    /**
     * @return the value of the ack level
     */
    public int getValue() {
        return value;
    }
}
