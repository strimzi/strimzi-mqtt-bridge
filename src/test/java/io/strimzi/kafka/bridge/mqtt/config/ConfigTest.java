/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.kafka.bridge.mqtt.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Some config related classes unit tests
 */
public class ConfigTest {

    @Test
    public void testConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put("bridge.id", "my-bridge");
        map.put("kafka.bootstrap.servers", "localhost:9092");
        map.put("kafka.producer.acks", "1");
        map.put("mqtt.host", "0.0.0.0");
        map.put("mqtt.port", "1883");

        BridgeConfig bridgeConfig = BridgeConfig.fromMap(map);
        assertThat(bridgeConfig.getBridgeID(), is("my-bridge"));

        // test no default topic set
        assertThat(bridgeConfig.getBridgeDefaultTopic(), is(BridgeConfig.BRIDGE_DEFAULT_TOPIC));

        // test no MQTT max bytes message set
        assertThat(bridgeConfig.getMqttConfig().getConfig().size(), is(2));
        assertThat(bridgeConfig.getMqttConfig().getMaxBytesMessage(), is(MqttConfig.DEFAULT_MQTT_MAX_BYTES_MESSAGE));

        map.put("bridge.topic.default", "default_topic");
        map.put("mqtt.max.bytes.message", "16384");

        bridgeConfig = BridgeConfig.fromMap(map);

        // test default topic and max bytes message set
        assertThat(bridgeConfig.getBridgeDefaultTopic(), is("default_topic"));

        assertThat(bridgeConfig.getKafkaConfig().getConfig().size(), is(1));
        assertThat(bridgeConfig.getKafkaConfig().getConfig().get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG), is("localhost:9092"));

        assertThat(bridgeConfig.getKafkaConfig().getProducerConfig().getConfig().size(), is(1));
        assertThat(bridgeConfig.getKafkaConfig().getProducerConfig().getConfig().get(ProducerConfig.ACKS_CONFIG), is("1"));

        assertThat(bridgeConfig.getMqttConfig().getConfig().size(), is(3));
        assertThat(bridgeConfig.getMqttConfig().getHost(), is("0.0.0.0"));
        assertThat(bridgeConfig.getMqttConfig().getPort(), is(1883));
        assertThat(bridgeConfig.getMqttConfig().getMaxBytesMessage(), is(16384));
    }

    @Test
    public void testHidingPassword() {
        String storePassword = "logged-config-should-not-contain-this-password";
        Map<String, Object> map = new HashMap<>();
        map.put("kafka.ssl.truststore.location", "/tmp/strimzi/bridge.truststore.p12");
        map.put("kafka.ssl.truststore.password", storePassword);
        map.put("kafka.ssl.truststore.type", "PKCS12");
        map.put("kafka.ssl.keystore.location", "/tmp/strimzi/bridge.keystore.p12");
        map.put("kafka.ssl.keystore.password", storePassword);
        map.put("kafka.ssl.keystore.type", "PKCS12");

        BridgeConfig bridgeConfig = BridgeConfig.fromMap(map);
        assertThat(bridgeConfig.getKafkaConfig().getConfig().size(), is(6));

        assertThat(bridgeConfig.getKafkaConfig().toString().contains("ssl.truststore.password=" + storePassword), is(false));
        assertThat(bridgeConfig.getKafkaConfig().toString().contains("ssl.truststore.password=[hidden]"), is(true));
    }

    @Test
    public void testMqttDefaults() {
        BridgeConfig bridgeConfig = BridgeConfig.fromMap(Map.of());

        assertThat(bridgeConfig.getMqttConfig().getHost(), is("0.0.0.0"));
        assertThat(bridgeConfig.getMqttConfig().getPort(), is(1883));
        assertThat(bridgeConfig.getMqttConfig().getMaxBytesMessage(), is(8092));

        MqttSslConfig sslConfig = bridgeConfig.getMqttConfig().getSslConfig();
        assertThat(sslConfig.isEnabled(), is(false));
        assertThat(sslConfig.getCertificate(), is(nullValue()));
        assertThat(sslConfig.getCertificateLocation(), is(nullValue()));
        assertThat(sslConfig.getKey(), is(nullValue()));
        assertThat(sslConfig.getKeyLocation(), is(nullValue()));
        assertThat(sslConfig.getEnabledProtocols(), is(MqttSslConfig.DEFAULT_MQTT_SSL_ENABLED_PROTOCOLS));
        assertThat(sslConfig.getEnabledCipherSuites(), is(List.of()));
    }

    @Test
    public void testMqttSslConfig() {
        String certificate = "-----BEGIN CERTIFICATE-----\ncert\n-----END CERTIFICATE-----";
        String key = "-----BEGIN PRIVATE KEY-----\nkey\n-----END PRIVATE KEY-----";
        Map<String, Object> map = new HashMap<>();
        map.put("mqtt.host", "0.0.0.0");
        map.put("mqtt.port", "8883");
        map.put("mqtt.ssl.enable", "true");
        map.put("mqtt.ssl.certificate", certificate);
        map.put("mqtt.ssl.certificate.location", "/tmp/server-cert.pem");
        map.put("mqtt.ssl.key", key);
        map.put("mqtt.ssl.key.location", "/tmp/server-key.pem");
        map.put("mqtt.ssl.enabled.protocols", "TLSv1.3");
        map.put("mqtt.ssl.enabled.cipher.suites", "TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384");

        BridgeConfig bridgeConfig = BridgeConfig.fromMap(map);
        MqttConfig mqttConfig = bridgeConfig.getMqttConfig();
        MqttSslConfig sslConfig = mqttConfig.getSslConfig();

        assertThat(mqttConfig.getConfig().size(), is(2));
        assertThat(sslConfig.getConfig().size(), is(7));
        assertThat(sslConfig.isEnabled(), is(true));
        assertThat(sslConfig.getCertificate(), is(certificate));
        assertThat(sslConfig.getCertificateLocation(), is("/tmp/server-cert.pem"));
        assertThat(sslConfig.getKey(), is(key));
        assertThat(sslConfig.getKeyLocation(), is("/tmp/server-key.pem"));
        assertThat(sslConfig.getEnabledProtocols(), is(List.of("TLSv1.3")));
        assertThat(sslConfig.getEnabledCipherSuites(), is(List.of("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384")));
    }

    @Test
    public void testHidingSslPrivateKey() {
        String key = "-----BEGIN PRIVATE KEY-----\nthis-should-not-be-logged\n-----END PRIVATE KEY-----";
        Map<String, Object> map = new HashMap<>();
        map.put("mqtt.ssl.enable", "true");
        map.put("mqtt.ssl.key", key);

        BridgeConfig bridgeConfig = BridgeConfig.fromMap(map);
        String sslConfigAsString = bridgeConfig.getMqttConfig().getSslConfig().toString();

        assertThat(sslConfigAsString.contains(key), is(false));
        assertThat(sslConfigAsString.contains("mqtt.ssl.key=[hidden]"), is(true));
    }
}

