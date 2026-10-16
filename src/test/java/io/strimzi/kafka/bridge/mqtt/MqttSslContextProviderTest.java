/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.ssl.SslHandler;
import io.strimzi.kafka.bridge.mqtt.config.MqttSslConfig;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLEngine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link MqttSslContextProvider}.
 */
public class MqttSslContextProviderTest {

    @Test
    public void testLoadFromPemFiles() throws Exception {
        MqttSslContextProvider provider = MqttSslContextProvider.load(sslConfigFromFiles());
        SslHandler handler = provider.newHandler(UnpooledByteBufAllocator.DEFAULT);
        SSLEngine engine = handler.engine();

        assertThat(handler, is(notNullValue()));
        assertThat(engine.getUseClientMode(), is(false));
        assertThat(engine.getEnabledProtocols(), hasItemInArray("TLSv1.2"));
        assertThat(engine.getEnabledProtocols(), hasItemInArray("TLSv1.3"));
    }

    @Test
    public void testLoadFromInlinePem() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        map.put(MqttSslConfig.MQTT_SSL_CERTIFICATE, SslTestUtils.readCertificatePem());
        map.put(MqttSslConfig.MQTT_SSL_KEY, SslTestUtils.readKeyPem());

        MqttSslContextProvider provider = MqttSslContextProvider.load(MqttSslConfig.fromMap(map));
        assertThat(provider.newHandler(UnpooledByteBufAllocator.DEFAULT), is(notNullValue()));
    }

    @Test
    public void testInlinePemTakesPrecedenceOverFileLocation() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        map.put(MqttSslConfig.MQTT_SSL_CERTIFICATE, SslTestUtils.readCertificatePem());
        map.put(MqttSslConfig.MQTT_SSL_KEY, SslTestUtils.readKeyPem());
        map.put(MqttSslConfig.MQTT_SSL_CERTIFICATE_LOCATION, "/this/path/does-not-exist.crt");
        map.put(MqttSslConfig.MQTT_SSL_KEY_LOCATION, "/this/path/does-not-exist.key");

        MqttSslContextProvider provider = MqttSslContextProvider.load(MqttSslConfig.fromMap(map));
        assertThat(provider.newHandler(UnpooledByteBufAllocator.DEFAULT), is(notNullValue()));
    }

    @Test
    public void testEnabledProtocolsAreApplied() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        map.put(MqttSslConfig.MQTT_SSL_CERTIFICATE_LOCATION, SslTestUtils.certificateLocation());
        map.put(MqttSslConfig.MQTT_SSL_KEY_LOCATION, SslTestUtils.keyLocation());
        map.put(MqttSslConfig.MQTT_SSL_ENABLED_PROTOCOLS, "TLSv1.3");

        MqttSslContextProvider provider = MqttSslContextProvider.load(MqttSslConfig.fromMap(map));
        SSLEngine engine = provider.newHandler(UnpooledByteBufAllocator.DEFAULT).engine();

        assertThat(List.of(engine.getEnabledProtocols()), is(List.of("TLSv1.3")));
    }

    @Test
    public void testMissingCertificateAndKeyThrows() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> MqttSslContextProvider.load(MqttSslConfig.fromMap(Map.of(MqttSslConfig.MQTT_SSL_ENABLE, "true"))));

        assertThat(exception.getMessage().contains("certificate"), is(true));
    }

    @Test
    public void testMissingKeyThrows() {
        Map<String, Object> map = new HashMap<>();
        map.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        map.put(MqttSslConfig.MQTT_SSL_CERTIFICATE_LOCATION, SslTestUtils.certificateLocation());

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> MqttSslContextProvider.load(MqttSslConfig.fromMap(map)));

        assertThat(exception.getMessage().contains("key"), is(true));
    }

    @Test
    public void testMissingCertificateThrows() {
        Map<String, Object> map = new HashMap<>();
        map.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        map.put(MqttSslConfig.MQTT_SSL_KEY_LOCATION, SslTestUtils.keyLocation());

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> MqttSslContextProvider.load(MqttSslConfig.fromMap(map)));

        assertThat(exception.getMessage().contains("certificate"), is(true));
    }

    @Test
    public void testMissingKeyFileThrows() {
        Map<String, Object> map = new HashMap<>();
        map.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        map.put(MqttSslConfig.MQTT_SSL_CERTIFICATE_LOCATION, SslTestUtils.certificateLocation());
        map.put(MqttSslConfig.MQTT_SSL_KEY_LOCATION, "/tmp/missing-server.key");

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> MqttSslContextProvider.load(MqttSslConfig.fromMap(map)));

        assertThat(exception.getMessage().contains("private key"), is(true));
    }

    @Test
    public void testInvalidPemThrows() {
        Map<String, Object> map = new HashMap<>();
        map.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        map.put(MqttSslConfig.MQTT_SSL_CERTIFICATE, "not-a-certificate");
        map.put(MqttSslConfig.MQTT_SSL_KEY, "not-a-private-key");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> MqttSslContextProvider.load(MqttSslConfig.fromMap(map)));
        assertThat(exception.getMessage().contains("valid certificates"), is(true));
    }

    @Test
    public void testMissingCertificateFileThrows() {
        Map<String, Object> map = new HashMap<>();
        map.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        map.put(MqttSslConfig.MQTT_SSL_CERTIFICATE_LOCATION, "/tmp/missing-server.crt");
        map.put(MqttSslConfig.MQTT_SSL_KEY_LOCATION, SslTestUtils.keyLocation());

        assertThrows(IllegalArgumentException.class, () -> MqttSslContextProvider.load(MqttSslConfig.fromMap(map)));
    }

    private static MqttSslConfig sslConfigFromFiles() {
        Map<String, Object> map = new HashMap<>();
        map.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        map.put(MqttSslConfig.MQTT_SSL_CERTIFICATE_LOCATION, SslTestUtils.certificateLocation());
        map.put(MqttSslConfig.MQTT_SSL_KEY_LOCATION, SslTestUtils.keyLocation());
        return MqttSslConfig.fromMap(map);
    }
}
