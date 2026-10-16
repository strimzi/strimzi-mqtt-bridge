/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.ssl.SslHandler;
import io.strimzi.kafka.bridge.mqtt.MqttSslContextProvider;
import io.strimzi.kafka.bridge.mqtt.SslTestUtils;
import io.strimzi.kafka.bridge.mqtt.config.BridgeConfig;
import io.strimzi.kafka.bridge.mqtt.config.MqttSslConfig;
import io.strimzi.kafka.bridge.mqtt.kafka.KafkaBridgeProducer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link MqttServerInitializer}.
 */
public class MqttServerInitializerTest {

    @Test
    public void testPipelineWithoutSsl() {
        BridgeConfig bridgeConfig = BridgeConfig.fromMap(Map.of());
        MqttServerInitializer initializer = new MqttServerInitializer(mock(KafkaBridgeProducer.class), bridgeConfig, null, List.of());

        EmbeddedChannel channel = new EmbeddedChannel(initializer);
        try {
            assertThat(channel.pipeline().get("ssl"), is(nullValue()));
            assertThat(channel.pipeline().get("decoder"), is(instanceOf(MqttDecoder.class)));
            assertThat(channel.pipeline().get("encoder"), is(instanceOf(MqttEncoder.class)));
            assertThat(channel.pipeline().get("handler"), is(instanceOf(MqttServerHandler.class)));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    public void testPipelineAddsSslHandlerBeforeMqttCodec() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(MqttSslConfig.MQTT_SSL_ENABLE, "true");
        map.put(MqttSslConfig.MQTT_SSL_CERTIFICATE_LOCATION, SslTestUtils.certificateLocation());
        map.put(MqttSslConfig.MQTT_SSL_KEY_LOCATION, SslTestUtils.keyLocation());
        BridgeConfig bridgeConfig = BridgeConfig.fromMap(map);

        MqttSslContextProvider sslContextProvider = MqttSslContextProvider.load(bridgeConfig.getMqttConfig().getSslConfig());
        MqttServerInitializer initializer = new MqttServerInitializer(
                mock(KafkaBridgeProducer.class), bridgeConfig, sslContextProvider, List.of());

        EmbeddedChannel channel = new EmbeddedChannel(initializer);
        try {
            List<String> names = channel.pipeline().names();
            assertThat(channel.pipeline().get("ssl"), is(instanceOf(SslHandler.class)));
            assertThat(channel.pipeline().get("decoder"), is(instanceOf(MqttDecoder.class)));
            assertThat(names.indexOf("ssl"), is(greaterThan(-1)));
            assertThat(names.indexOf("decoder") > names.indexOf("ssl"), is(true));
            assertThat(names.indexOf("encoder") > names.indexOf("decoder"), is(true));
            assertThat(names.indexOf("handler") > names.indexOf("encoder"), is(true));
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
