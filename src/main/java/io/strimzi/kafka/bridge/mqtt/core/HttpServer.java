/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Jetty based HTTP server used for health checks
 */
public class HttpServer {

    private static final Logger LOGGER = LogManager.getLogger(HttpServer.class);
    private static final int HTTP_PORT = 8080;

    private final Server server;
    private final Liveness liveness;
    private final Readiness readiness;

    /**
     * Constructs the health check HTTP server.
     *
     * @param liveness  Callback used for the health check.
     * @param readiness Callback used for the readiness check.
     */
    public HttpServer(Liveness liveness, Readiness readiness) {
        this.liveness = liveness;
        this.readiness = readiness;

        this.server = new Server(HTTP_PORT);

        ContextHandler readinessContext = new ContextHandler("/ready");
        readinessContext.setHandler(new ReadyHandler());
        readinessContext.setAllowNullPathInfo(true);

        ContextHandler livenessContext = new ContextHandler("/healthy");
        livenessContext.setHandler(new HealthyHandler());
        livenessContext.setAllowNullPathInfo(true);

        server.setHandler(new ContextHandlerCollection(readinessContext, livenessContext));
    }

    /**
     * Start the HTTP server
     */
    public void start() {
        try {
            this.server.start();
        } catch (Exception e)   {
            LOGGER.error("Failed to start the HTTP server", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Stop the HTTP server
     */
    public void stop() {
        try {
            this.server.stop();
        } catch (Exception e)   {
            LOGGER.error("Failed to stop the HTTP server", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Handler responsible for the liveness check
     */
    class HealthyHandler extends AbstractHandler {

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
            if (liveness.isAlive()) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            LOGGER.debug("Responding {} to GET /healthy", response.getStatus());
            baseRequest.setHandled(true);
        }
    }

    /**
     * Handler responsible for the readiness check
     */
    class ReadyHandler extends AbstractHandler {

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
            if (readiness.isReady()) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            LOGGER.debug("Responding {} to GET /ready", response.getStatus());
            baseRequest.setHandled(true);
        }
    }
}
