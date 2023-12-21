/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

/**
 * A liveness check implemented by an application and called by the
 * {@link HttpServer} when handling a health check request.
 */
public interface Liveness {

    /**
     * @return  True when the application is alive, false otherwise.
     */
    boolean isAlive();
}
