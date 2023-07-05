/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.utils;

/**
 * Represents the MQTT message QoS
 */
public enum MqttMessageQoS {
    // QoS 0: At most once delivery
    AT_MOST_ONCE(0),
    // QoS 1: At least once delivery
    AT_LEAST_ONCE(1),
    // QoS 2: Exactly once delivery
    EXACTLY_ONCE(2);
    private final int value;

    /**
     * Constructor
     *
     * @param value the value of the QoS
     */
    MqttMessageQoS(int value) {
        this.value = value;
    }

    /**
     * Get the QoS from the value
     *
     * @param value the value of the QoS
     * @return the QoS
     */
    public static MqttMessageQoS valueOf(int value) {
        return switch (value) {
            case 0 -> AT_MOST_ONCE;
            case 1 -> AT_LEAST_ONCE;
            case 2 -> EXACTLY_ONCE;
            default -> throw new IllegalArgumentException("Unknown QoS value: " + value);
        };
    }

    /**
     * @return the value of the QoS
     */
    public int getValue() {
        return value;
    }
}
