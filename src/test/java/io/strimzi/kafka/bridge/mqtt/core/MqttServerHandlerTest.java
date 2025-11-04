/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessageFactory;
import io.strimzi.kafka.bridge.mqtt.kafka.KafkaBridgeProducer;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRulesLoader;
import java.util.Objects;
import org.junit.jupiter.api.Test;

public class MqttServerHandlerTest {

    @Test
    public void testReadMessageWithDecodingError() {
        String mappingRulesPath =
            Objects.requireNonNull(getClass().getClassLoader().getResource("mapping-rules-regex.json"))
                .getPath();
        MappingRulesLoader.getInstance().init(mappingRulesPath);

        KafkaBridgeProducer producer = mock(KafkaBridgeProducer.class);
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Throwable error = mock(Throwable.class);

        MqttServerHandler handler = new MqttServerHandler(producer, "default-topic");
        handler.channelRead0(ctx, MqttMessageFactory.newInvalidMessage(error));

        verify(error, times(1)).getMessage();
        verify(error, times(1)).printStackTrace();
        verify(ctx, times(1)).close();

        verifyNoMoreInteractions(producer);
        verifyNoMoreInteractions(error);
        verifyNoMoreInteractions(ctx);
    }
}
