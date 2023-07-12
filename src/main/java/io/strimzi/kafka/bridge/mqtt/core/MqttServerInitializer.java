/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.strimzi.kafka.bridge.mqtt.kafka.BridgeKafkaProducerService;

/**
 * This helper class help us add necessary Netty pipelines handlers. <br>
 * During the {@link #initChannel(SocketChannel)}, we use MqttDecoder() and MqttEncoder to decode and encode Mqtt messages respectively. <br>
 */
public class MqttServerInitializer extends ChannelInitializer<SocketChannel> {
    private final MqttServerHandler mqttServerHandler;

    public MqttServerInitializer(BridgeKafkaProducerService producerService) {
        this.mqttServerHandler = new MqttServerHandler(producerService);
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast("decoder", new MqttDecoder());
        ch.pipeline().addLast("encoder", MqttEncoder.INSTANCE);
        ch.pipeline().addLast("handler", this.mqttServerHandler);
    }
}
