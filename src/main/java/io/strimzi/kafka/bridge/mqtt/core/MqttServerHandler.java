/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRule;
import io.strimzi.kafka.bridge.mqtt.mapper.MqttKafkaMapper;
import io.strimzi.kafka.bridge.mqtt.utils.MappingRulesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Represents a SimpleChannelInboundHandler. The MqttServerHandler is responsible for: <br>
 * - listen to client connections;<br>
 * - listen to incoming messages; <br>
 *
 * @see io.netty.channel.SimpleChannelInboundHandler
 */
public class MqttServerHandler extends SimpleChannelInboundHandler<MqttMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MqttServerHandler.class);
    private MqttKafkaMapper mqttKafkaMapper;

    /**
     * Constructor
     */
    public MqttServerHandler() {
        super(false);
        try {
            MappingRulesLoader mappingRulesLoader = MappingRulesLoader.getInstance();
            List<MappingRule> rules = mappingRulesLoader.loadRules();
            this.mqttKafkaMapper = new MqttKafkaMapper(rules);
        } catch (IOException e) {
            logger.error("Error reading mapping file: ", e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("Client  {} is trying to connect", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
        try {
            MqttMessageType messageType = msg.fixedHeader().messageType();
            logger.debug("Got {} message type", messageType.name());
            if (messageType == MqttMessageType.CONNECT) {
                handleConnectMessage(ctx);
            } else if (messageType == MqttMessageType.PUBLISH) {
                MqttPublishMessage message = (MqttPublishMessage) msg;
                message.retain();
                handlePublishMessage(ctx, message);
                message.release();
            }
        } catch (Exception e) {
            logger.error("Error reading Channel: ", e);
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * Handle the case when a client sent a MQTT CONNECT message type.
     *
     * @param ctx ChannelHandlerContext instance
     */
    private void handleConnectMessage(ChannelHandlerContext ctx) {
        MqttConnAckMessage connAckMessage = MqttMessageBuilders.connAck()
                .sessionPresent(false)
                .returnCode(MqttConnectReturnCode.CONNECTION_ACCEPTED)
                .build();

        logger.info("{} client connected.", ctx.channel().remoteAddress());
        ctx.writeAndFlush(connAckMessage);
    }

    /**
     * Handle the case when a client sent a MQTT PUBLISH message type.
     *
     * @param ctx            ChannelHandlerContext instance
     * @param publishMessage represents a MqttPublishMessage
     */
    private void handlePublishMessage(ChannelHandlerContext ctx, MqttPublishMessage publishMessage) {
        logger.info("MQTT Topic: {}", publishMessage.variableHeader().topicName());
        logger.info("Kafka Mapped Topic: {}", mqttKafkaMapper.map(publishMessage.variableHeader().topicName()));
        logger.info("Message: {}", publishMessage.payload().toString(Charset.defaultCharset()));
    }
}
