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
import io.netty.handler.codec.mqtt.MqttQoS;
import io.strimzi.kafka.bridge.mqtt.kafka.KafkaBridgeProducer;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRule;
import io.strimzi.kafka.bridge.mqtt.mapper.MqttKafkaMapper;
import io.strimzi.kafka.bridge.mqtt.utils.MappingRulesLoader;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static io.netty.channel.ChannelHandler.Sharable;

/**
 * Represents a SimpleChannelInboundHandler. The MqttServerHandler is responsible for: <br>
 * - listen to client connections;<br>
 * - listen to incoming messages; <br>
 *
 * @see io.netty.channel.SimpleChannelInboundHandler
 */
@Sharable
public class MqttServerHandler extends SimpleChannelInboundHandler<MqttMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MqttServerHandler.class);
    private final KafkaBridgeProducer kafkaBridgeProducer;
    private MqttKafkaMapper mqttKafkaMapper;

    /**
     * Constructor
     */
    public MqttServerHandler(KafkaBridgeProducer kafkaBridgeProducer) {
        // auto release reference count to avoid memory leak
        super(true);
        try {
            MappingRulesLoader mappingRulesLoader = MappingRulesLoader.getInstance();
            List<MappingRule> rules = mappingRulesLoader.loadRules();
            this.mqttKafkaMapper = new MqttKafkaMapper(rules);
        } catch (IOException e) {
            logger.error("Error reading mapping file: ", e);
        }
        this.kafkaBridgeProducer = kafkaBridgeProducer;
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
     * Send a MQTT PUBACK message to the client.
     *
     * @param ctx ChannelHandlerContext instance
     * @param packetId packet identifier
     */
    private void sendPubAckMessage(ChannelHandlerContext ctx, int packetId) {
        MqttMessage pubAckMessage = MqttMessageBuilders.pubAck()
                .packetId(packetId)
                .build();

        ctx.writeAndFlush(pubAckMessage);
    }

    /**
     * Transform a MqttPublishMessage's payload into byte array
     */
    private static byte[] payloadToBytes(MqttPublishMessage msg) {
        byte[] data = new byte[msg.payload().readableBytes()];
        msg.payload().readBytes(data);
        return data;
    }

    /**
     * Handle the case when a client sent a MQTT PUBLISH message type.
     *
     * @param ctx            ChannelHandlerContext instance
     * @param publishMessage represents a MqttPublishMessage
     */
    private void handlePublishMessage(ChannelHandlerContext ctx, MqttPublishMessage publishMessage) {
        // get QoS level from the MqttPublishMessage
        MqttQoS qos = MqttQoS.valueOf(publishMessage.fixedHeader().qosLevel().value());

        // get the MQTT topic from the MqttPublishMessage
        String mqttTopic = publishMessage.variableHeader().topicName();

        // perform topic mapping
        String kafkaMappedTopic = mqttKafkaMapper.map(mqttTopic);

        //log the topic mapping
        logger.info("MQTT topic {} mapped to Kafka Topic {}", mqttTopic, kafkaMappedTopic);

        byte[] data = payloadToBytes(publishMessage);
        // build the Kafka record
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(kafkaMappedTopic,
                data);

        // send the record to the Kafka topic
        switch (qos) {
            case AT_MOST_ONCE -> {
                kafkaBridgeProducer.sendNoAck(record);
                logger.info("Message sent to Kafka on topic {}", record.topic());
            }
            case AT_LEAST_ONCE -> {
                CompletionStage<RecordMetadata> result = kafkaBridgeProducer.send(record);
                // wait for the result of the send operation
                result.whenComplete((metadata, error) -> {
                    if (error != null) {
                        logger.error("Error sending message to Kafka: ", error);
                    } else {
                        logger.info("Message sent to Kafka on topic {} with offset {}", metadata.topic(), metadata.offset());
                        // send PUBACK message to the client
                        sendPubAckMessage(ctx, publishMessage.variableHeader().packetId());
                    }
                });
            }
            case EXACTLY_ONCE -> logger.warn("QoS level EXACTLY_ONCE is not supported");
        }
    }
}
