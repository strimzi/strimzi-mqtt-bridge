/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.kafka.bridge.mqtt.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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

        map.put("bridge.topic.default", "default_topic");

        bridgeConfig = BridgeConfig.fromMap(map);

        // test default topic set
        assertThat(bridgeConfig.getBridgeDefaultTopic(), is("default_topic"));

        assertThat(bridgeConfig.getKafkaConfig().getConfig().size(), is(1));
        assertThat(bridgeConfig.getKafkaConfig().getConfig().get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG), is("localhost:9092"));

        assertThat(bridgeConfig.getKafkaConfig().getProducerConfig().getConfig().size(), is(1));
        assertThat(bridgeConfig.getKafkaConfig().getProducerConfig().getConfig().get(ProducerConfig.ACKS_CONFIG), is("1"));

        assertThat(bridgeConfig.getMqttConfig().getConfig().size(), is(2));
        assertThat(bridgeConfig.getMqttConfig().getHost(), is("0.0.0.0"));
        assertThat(bridgeConfig.getMqttConfig().getPort(), is(1883));
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
    }
}

