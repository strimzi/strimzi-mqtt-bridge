/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.strimzi.kafka.bridge.mqtt.kafka.KafkaBridgeProducer;

/**
 * This helper class help us add necessary Netty pipelines handlers. <br>
 * During the {@link #initChannel(SocketChannel)}, we use MqttDecoder() and MqttEncoder to decode and encode Mqtt messages respectively. <br>
 */
public class MqttServerInitializer extends ChannelInitializer<SocketChannel> {
    private final MqttServerHandler mqttServerHandler;
    private final int decoderMaxBytesInMessage;

    /**
     * Constructor
     *
     * @param kafkaBridgeProducer   instance of the Kafka producer for sending messages
     * @param bridgeDefaultTopic    default Kafka topic to be used if there are no matches for the MQTT topic pattern
     * @param decoderMaxBytesInMessage  maximum number of bytes for the MQTT request during decoding
     */
    public MqttServerInitializer(KafkaBridgeProducer kafkaBridgeProducer, String bridgeDefaultTopic, int decoderMaxBytesInMessage) {
        this.mqttServerHandler = new MqttServerHandler(kafkaBridgeProducer, bridgeDefaultTopic);
        this.decoderMaxBytesInMessage = decoderMaxBytesInMessage;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast("decoder", new MqttDecoder(decoderMaxBytesInMessage));
        ch.pipeline().addLast("encoder", MqttEncoder.INSTANCE);
        ch.pipeline().addLast("handler", this.mqttServerHandler);
    }
}
