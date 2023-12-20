/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

/**
 * A readiness check implemented by an application and called by the
 * {@link HttpServer} when handling a health check request.
 */
public interface Readiness {

    /**
     * @return  True when the application is ready, false otherwise
     */
    boolean isReady();
}
