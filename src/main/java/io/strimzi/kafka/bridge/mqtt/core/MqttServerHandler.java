/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.strimzi.kafka.bridge.mqtt.config.KafkaConfig;
import io.strimzi.kafka.bridge.mqtt.kafka.BridgeKafkaProducer;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRule;
import io.strimzi.kafka.bridge.mqtt.mapper.MqttKafkaMapper;
import io.strimzi.kafka.bridge.mqtt.utils.KafkaProducerAckLevel;
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
    private MqttKafkaMapper mqttKafkaMapper;
    // A Kafka Producer to handle  mqtt messages with QoS 0
    private final BridgeKafkaProducer bridgeKafkaProducerZero;
    //  A Kafka Producer to handle  mqtt messages with QoS 1
    private final BridgeKafkaProducer bridgeKafkaProducerOne;

    /**
     * Constructor
     */
    public MqttServerHandler(KafkaConfig kafkaConfig) {
        // auto release reference count to avoid memory leak
        super(true);
        try {
            MappingRulesLoader mappingRulesLoader = MappingRulesLoader.getInstance();
            List<MappingRule> rules = mappingRulesLoader.loadRules();
            this.mqttKafkaMapper = new MqttKafkaMapper(rules);
        } catch (IOException e) {
            logger.error("Error reading mapping file: ", e);
        }
        this.bridgeKafkaProducerZero = new BridgeKafkaProducer(kafkaConfig, KafkaProducerAckLevel.ZERO);
        this.bridgeKafkaProducerOne = new BridgeKafkaProducer(kafkaConfig, KafkaProducerAckLevel.ONE);
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
        this.bridgeKafkaProducerOne.close();
        this.bridgeKafkaProducerZero.close();
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
     * Handle the case when a client sent a MQTT PUBLISH message type.
     *
     * @param ctx            ChannelHandlerContext instance
     * @param publishMessage represents a MqttPublishMessage
     */
    private void handlePublishMessage(ChannelHandlerContext ctx, MqttPublishMessage publishMessage) {
        // get QoS level from the MqttPublishMessage
        MqttQoS qos = MqttQoS.valueOf(publishMessage.fixedHeader().qosLevel().value());

        // perform topic mapping
        String mappedTopic = mqttKafkaMapper.map(publishMessage.variableHeader().topicName());

        // build the Kafka record
        ProducerRecord<String, ByteBuf> record = new ProducerRecord<>(mappedTopic,
                publishMessage.payload());

        // get the appropriate Kafka producer according to the QoS level
        BridgeKafkaProducer producer = this.getProducer(qos);

        // send the record to the Kafka topic
        switch (qos) {
            case AT_MOST_ONCE -> {
                producer.send(record);
                logger.info("Message sent to Kafka on topic {}", record.topic());
            }
            case AT_LEAST_ONCE -> {
                CompletionStage<RecordMetadata> result = producer.send(record);
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
            case EXACTLY_ONCE -> logger.warn("QoS level EXACTLY_ONCE is not supported yet");
        }
    }

    /**
     * Get the Kafka producer for the given Mqtt QoS
     *
     * @param qos Mqtt QoS
     * @return BridgeKafkaProducer
     */
    public BridgeKafkaProducer getProducer(MqttQoS qos) {

        return switch (qos) {
            case AT_MOST_ONCE -> this.bridgeKafkaProducerZero;
            case AT_LEAST_ONCE -> this.bridgeKafkaProducerOne;
            case EXACTLY_ONCE -> null;
            case FAILURE -> throw new IllegalStateException("Unexpected value: " + qos);
        };
    }
}
