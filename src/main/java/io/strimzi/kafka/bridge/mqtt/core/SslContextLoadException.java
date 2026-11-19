/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

/**
 * Exception thrown when the SSL context cannot be loaded.
 */
public class SslContextLoadException extends RuntimeException {
    /**
     * Constructor
     *
     * @param cause the underlying cause of the SSL context loading failure
     */
    public SslContextLoadException(Throwable cause) {
        super(cause);
    }
}
