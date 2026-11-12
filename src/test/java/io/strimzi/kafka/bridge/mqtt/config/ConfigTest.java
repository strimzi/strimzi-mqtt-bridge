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
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
        assertThat(sslConfig.getEnabledProtocols(), is(Set.of("TLSv1.2", "TLSv1.3")));
        assertThat(sslConfig.getEnabledCipherSuites(), is(nullValue()));
        assertThat(sslConfig.getNamedGroups(), is(nullValue()));
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
        map.put("mqtt.ssl.enabled.cipher.suites", "TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384");
        map.put("mqtt.ssl.named.groups", "x25519mlkem768,x25519,secp256r1,secp384r1");

        BridgeConfig bridgeConfig = BridgeConfig.fromMap(map);
        MqttConfig mqttConfig = bridgeConfig.getMqttConfig();
        MqttSslConfig sslConfig = mqttConfig.getSslConfig();

        assertThat(mqttConfig.getConfig().size(), is(2));
        assertThat(sslConfig.getConfig().size(), is(8));
        assertThat(sslConfig.isEnabled(), is(true));
        assertThat(sslConfig.getCertificate(), is(certificate));
        assertThat(sslConfig.getCertificateLocation(), is("/tmp/server-cert.pem"));
        assertThat(sslConfig.getKey(), is(key));
        assertThat(sslConfig.getKeyLocation(), is("/tmp/server-key.pem"));
        assertThat(sslConfig.getEnabledProtocols(), is(Set.of("TLSv1.3")));
        assertThat(sslConfig.getEnabledCipherSuites(), is(Set.of("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384")));
        assertThat(sslConfig.getNamedGroups(), is(List.of("x25519mlkem768", "x25519", "secp256r1", "secp384r1")));
    }

    @Test
    public void testHideSecretsKafkaConfig() {
        String pemKey = "FAKE-PEM-PRIVATE-KEY";
        String certChain = "FAKE-PEM-CERT-CHAIN";
        String truststoreCert = "FAKE-PEM-TRUSTSTORE-CERT";
        String jaasConfig = "PlainLoginModule required username=\"alice\" password=\"secret\";";
        String oauthSecret = "FAKE-OAUTH-SECRET";
        String passphrase = "FAKE-PASSPHRASE";

        Map<String, Object> map = Map.of(
                "kafka.ssl.keystore.key", pemKey,
                "kafka.ssl.keystore.certificate.chain", certChain,
                "kafka.ssl.truststore.certificates", truststoreCert,
                "kafka.sasl.jaas.config", jaasConfig,
                "kafka.sasl.oauthbearer.client.credentials.client.secret", oauthSecret,
                "kafka.sasl.oauthbearer.assertion.private.key.passphrase", passphrase
        );

        String out = BridgeConfig.fromMap(map).getKafkaConfig().toString();

        assertThat(out, not(containsString(pemKey)));
        assertThat(out, not(containsString(certChain)));
        assertThat(out, not(containsString(truststoreCert)));
        assertThat(out, not(containsString(jaasConfig)));
        assertThat(out, not(containsString(oauthSecret)));
        assertThat(out, not(containsString(passphrase)));

        assertThat(out, containsString("ssl.keystore.key=[hidden]"));
        assertThat(out, containsString("ssl.keystore.certificate.chain=[hidden]"));
        assertThat(out, containsString("ssl.truststore.certificates=[hidden]"));
        assertThat(out, containsString("sasl.jaas.config=[hidden]"));
        assertThat(out, containsString("sasl.oauthbearer.client.credentials.client.secret=[hidden]"));
        assertThat(out, containsString("sasl.oauthbearer.assertion.private.key.passphrase=[hidden]"));
    }

    @Test
    public void testHideSecretsKafkaProducerConfig() {
        String keystorePassword = "FAKE-KEYSTORE-PASSWORD-PRODUCER";
        String pemKey = "FAKE-PEM-KEY-PRODUCER";
        String jaasConfig = "PlainLoginModule required username=\"producer\" password=\"secret\";";
        String oauthSecret = "FAKE-OAUTH-SECRET-PRODUCER";

        Map<String, Object> map = Map.of(
                "kafka.producer.ssl.keystore.password", keystorePassword,
                "kafka.producer.ssl.keystore.key", pemKey,
                "kafka.producer.sasl.jaas.config", jaasConfig,
                "kafka.producer.sasl.oauthbearer.client.credentials.client.secret", oauthSecret
        );

        String out = BridgeConfig.fromMap(map).getKafkaConfig().getProducerConfig().toString();

        assertThat(out, not(containsString(keystorePassword)));
        assertThat(out, not(containsString(pemKey)));
        assertThat(out, not(containsString(jaasConfig)));
        assertThat(out, not(containsString(oauthSecret)));

        assertThat(out, containsString("ssl.keystore.password=[hidden]"));
        assertThat(out, containsString("ssl.keystore.key=[hidden]"));
        assertThat(out, containsString("sasl.jaas.config=[hidden]"));
        assertThat(out, containsString("sasl.oauthbearer.client.credentials.client.secret=[hidden]"));
    }

    @Test
    public void testHideSecretsMqttSslConfig() {
        String pemKey = "FAKE-MQTT-SERVER-PEM-KEY";
        String pemCert = "FAKE-MQTT-SERVER-PEM-CERT";

        Map<String, Object> map = Map.of(
                "mqtt.ssl.enable", "true",
                "mqtt.ssl.key", pemKey,
                "mqtt.ssl.certificate", pemCert
        );

        String out = BridgeConfig.fromMap(map).getMqttConfig().getSslConfig().toString();

        assertThat(out, not(containsString(pemKey)));
        assertThat(out, not(containsString(pemCert)));

        assertThat(out, containsString("mqtt.ssl.key=[hidden]"));
        assertThat(out, containsString("mqtt.ssl.certificate=[hidden]"));
    }

    @Test
    public void testNonSensitiveKeysNotHidden() {
        Map<String, Object> map = Map.of(
                "kafka.bootstrap.servers", "localhost:9092",
                "kafka.ssl.truststore.type", "PEM",
                "kafka.ssl.keystore.type", "PEM",
                "kafka.producer.acks", "1",
                "mqtt.host", "0.0.0.0",
                "mqtt.port", "8883",
                "mqtt.ssl.enable", "true",
                "mqtt.ssl.key.location", "/opt/strimzi/certs/tls.key",
                "mqtt.ssl.certificate.location", "/opt/strimzi/certs/tls.crt"
        );

        BridgeConfig bridgeConfig = BridgeConfig.fromMap(map);
        String kafkaOut = bridgeConfig.getKafkaConfig().toString();
        String mqttOut = bridgeConfig.getMqttConfig().toString();

        // non-sensitive kafka values must remain visible
        assertThat(kafkaOut, containsString("bootstrap.servers=localhost:9092"));
        assertThat(kafkaOut, containsString("ssl.truststore.type=PEM"));
        assertThat(kafkaOut, containsString("ssl.keystore.type=PEM"));
        assertThat(kafkaOut, containsString("acks=1"));

        // non-sensitive mqtt values and file paths are not sensitive
        assertThat(mqttOut, containsString("mqtt.host=0.0.0.0"));
        assertThat(mqttOut, containsString("mqtt.port=8883"));
        assertThat(mqttOut, containsString("mqtt.ssl.key.location=/opt/strimzi/certs/tls.key"));
        assertThat(mqttOut, containsString("mqtt.ssl.certificate.location=/opt/strimzi/certs/tls.crt"));
        assertThat(mqttOut, not(containsString("[hidden]")));
    }
}

