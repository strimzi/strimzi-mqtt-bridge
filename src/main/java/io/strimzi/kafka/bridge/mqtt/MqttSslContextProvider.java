/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.strimzi.kafka.bridge.mqtt.config.MqttSslConfig;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides a reusable Netty {@link SslContext} for MQTT connections.
 * <p>
 * The SSL context is created once when this provider is initialized and is
 * reused for all subsequent connections. This avoids recreating the relatively
 * expensive SSL configuration for every connection while allowing a new
 * {@link SslHandler} to be created for each connection.
 */
public class MqttSslContextProvider {
    private final SslContext sslContext;

    private MqttSslContextProvider(SslContext sslContext) {
        this.sslContext = sslContext;
    }

    /**
     * Loads an SSL context provider based on the provided SSL configuration.
     */
    public static MqttSslContextProvider load(MqttSslConfig sslConfig) throws SSLException {
        var certChain = new ByteArrayInputStream(resolveCertificate(sslConfig));
        var key = new ByteArrayInputStream(resolvePrivateKey(sslConfig));

        SslContext sslContext = SslContextBuilder.forServer(certChain, key)
                .protocols(sslConfig.getEnabledProtocols())
                .ciphers(sslConfig.getEnabledCipherSuites().isEmpty() ? null : sslConfig.getEnabledCipherSuites())
                .build();

        return new MqttSslContextProvider(sslContext);
    }

    /**
     * Creates a new SSL handler for a connection.
     *
     * @param allocator the allocator used to create the handler
     * @return a new SSL handler backed by the shared SSL context
     */
    public SslHandler newHandler(ByteBufAllocator allocator) {
        return sslContext.newHandler(allocator);
    }

    /**
     * Util method to resolve ssl private key.
     * Precedence is given inline private key.
     *
     * @param mqttSslConfig ssl configuration
     * @return byte array of private key
     */
    private static byte[] resolvePrivateKey(MqttSslConfig mqttSslConfig) {
        if (mqttSslConfig.getKey() != null) {
            return mqttSslConfig.getKey().getBytes(StandardCharsets.US_ASCII);
        }

        if (mqttSslConfig.getKeyLocation() != null) {
            try {
                return Files.readAllBytes(Path.of(mqttSslConfig.getKeyLocation()));
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read private key from location: " + mqttSslConfig.getKeyLocation(), e);
            }
        }

        throw new IllegalArgumentException("Either 'key' or 'key.location' must be provided in the SSL configuration.");
    }

    /**
     * Util method to resolve key certificate.
     * Precedence is given to inline certificate.
     *
     * @param mqttSslConfig ssl configuration
     * @return byte array of certificate.
     */
    private static byte[] resolveCertificate(MqttSslConfig mqttSslConfig) {
        if (mqttSslConfig.getCertificate() != null) {
            return mqttSslConfig.getCertificate().getBytes(StandardCharsets.US_ASCII);
        }

        if (mqttSslConfig.getCertificateLocation() != null) {
            try {
                return Files.readAllBytes(Path.of(mqttSslConfig.getCertificateLocation()));
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read certificate from location: " + mqttSslConfig.getCertificateLocation(), e);
            }
        }

        throw new IllegalArgumentException("Either 'certificate' or 'certificate.location' must be provided in the SSL configuration.");
    }
}
