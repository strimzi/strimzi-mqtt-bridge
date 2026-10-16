/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * Shared helpers for MQTT SSL/TLS tests.
 */
public final class SslTestUtils {
    private SslTestUtils() {
    }

    /**
     * @return filesystem path of the test server certificate
     */
    public static String certificateLocation() {
        return resourcePath("ssl/server.crt");
    }

    /**
     * @return filesystem path of the test server private key
     */
    public static String keyLocation() {
        return resourcePath("ssl/server.key");
    }

    /**
     * @return PEM contents of the test server certificate
     */
    public static String readCertificatePem() throws Exception {
        return Files.readString(Path.of(certificateLocation()));
    }

    /**
     * @return PEM contents of the test server private key
     */
    public static String readKeyPem() throws Exception {
        return Files.readString(Path.of(keyLocation()));
    }

    /**
     * Builds a client socket factory that trusts the test server certificate.
     *
     * @return SSL socket factory for MQTT TLS clients
     */
    public static SSLSocketFactory trustServerCertificateSocketFactory() throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate;
        try (InputStream is = Files.newInputStream(Path.of(certificateLocation()))) {
            certificate = (X509Certificate) certificateFactory.generateCertificate(is);
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null);
        trustStore.setCertificateEntry("mqtt-server", certificate);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }

    private static String resourcePath(String name) {
        return Objects.requireNonNull(SslTestUtils.class.getClassLoader().getResource(name)).getPath();
    }
}
