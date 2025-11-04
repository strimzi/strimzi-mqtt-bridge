/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.strimzi.kafka.bridge.mqtt.kafka.KafkaBridgeProducer;
import io.strimzi.kafka.bridge.mqtt.mapper.MqttKafkaMapper;
import io.strimzi.kafka.bridge.mqtt.mapper.MqttKafkaRegexMapper;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRule;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingResult;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRulesLoader;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
@SuppressWarnings({"checkstyle:ClassFanOutComplexity"})
@Sharable
public class MqttServerHandler extends SimpleChannelInboundHandler<MqttMessage> {
    private static final Logger LOGGER = LogManager.getLogger(MqttServerHandler.class);
    private final KafkaBridgeProducer kafkaBridgeProducer;
    private MqttKafkaMapper mqttKafkaMapper;

    /**
     * Constructor
     *
     * @param kafkaBridgeProducer   instance of the Kafka producer for sending messages
     * @param bridgeDefaultTopic    default Kafka topic to be used if there are no matches for the MQTT topic pattern
     */
    public MqttServerHandler(KafkaBridgeProducer kafkaBridgeProducer, String bridgeDefaultTopic) {
        // auto release reference count to avoid memory leak
        super(true);
        try {
            MappingRulesLoader mappingRulesLoader = MappingRulesLoader.getInstance();
            List<MappingRule> rules = mappingRulesLoader.loadRules();
            this.mqttKafkaMapper = new MqttKafkaRegexMapper(rules, bridgeDefaultTopic);
        } catch (IOException e) {
            LOGGER.error("Error reading mapping file: ", e);
        }
        this.kafkaBridgeProducer = kafkaBridgeProducer;
    }

    /**
     * Transform a MqttPublishMessage's payload into bytes array
     */
    private static byte[] payloadToBytes(MqttPublishMessage msg) {
        byte[] data = new byte[msg.payload().readableBytes()];
        msg.payload().readBytes(data);
        return data;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("Client  {} is trying to connect", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
        if (msg.decoderResult().isFailure()) {
            Throwable error = msg.decoderResult().cause();
            LOGGER.error("Received invalid message with error: {}", error.getMessage());
            exceptionCaught(ctx, error);
            return;
        }
        try {
            MqttMessageType messageType = msg.fixedHeader().messageType();
            LOGGER.debug("Got {} message type", messageType.name());
            if (msg instanceof MqttConnectMessage) {
                handleConnectMessage(ctx, (MqttConnectMessage) msg);
            } else if (msg instanceof MqttPublishMessage) {
                handlePublishMessage(ctx, (MqttPublishMessage) msg);
            } else if (messageType == MqttMessageType.PINGREQ) {
                handlePingReqMessage(ctx, msg);
            } else {
                LOGGER.warn("Message type {} not handled", messageType.name());
            }
        } catch (Exception e) {
            LOGGER.error("Error reading Channel: ", e);
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
     * @param connectMessage incoming MqttConnectMessage
     */
    private void handleConnectMessage(ChannelHandlerContext ctx, MqttConnectMessage connectMessage) {
        MqttConnAckMessage connAckMessage = MqttMessageBuilders.connAck()
                .sessionPresent(false)
                .returnCode(MqttConnectReturnCode.CONNECTION_ACCEPTED)
                .build();

        LOGGER.info("Client [{}] connected from {}", connectMessage.payload().clientIdentifier(), ctx.channel().remoteAddress());
        ctx.writeAndFlush(connAckMessage);
    }

    /**
     * Handle the case when a client sent a MQTT PINGREQ message type.
     *
     * @param ctx ChannelHandlerContext instance
     * @param pingreqMessage incoming MqttMessage
     */
    private void handlePingReqMessage(ChannelHandlerContext ctx, MqttMessage pingreqMessage) {
        MqttFixedHeader pingreqFixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false,
                                                                     MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage pingResp = new MqttMessage(pingreqFixedHeader);
        ctx.writeAndFlush(pingResp);
    }

    /**
     * Send a MQTT PUBACK message to the client.
     *
     * @param ctx      ChannelHandlerContext instance
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

        // get the MQTT topic from the MqttPublishMessage
        String mqttTopic = publishMessage.variableHeader().topicName();

        // perform topic mapping
        MappingResult mappingResult = mqttKafkaMapper.map(mqttTopic);

        // log the topic mapping
        LOGGER.info("MQTT topic {} mapped to Kafka Topic {} with Key {}", mqttTopic, mappingResult.kafkaTopic(), mappingResult.kafkaKey());

        byte[] data = payloadToBytes(publishMessage);
        Headers headers = new RecordHeaders();
        headers.add(new RecordHeader("mqtt-topic", mqttTopic.getBytes(StandardCharsets.UTF_8)));
        // build the Kafka record
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(mappingResult.kafkaTopic(), null, mappingResult.kafkaKey(),
                data, headers);

        // send the record to the Kafka topic
        switch (qos) {
            case AT_MOST_ONCE -> {
                kafkaBridgeProducer.sendNoAck(record);
                LOGGER.info("Message sent to Kafka on topic {}", record.topic());
            }
            case AT_LEAST_ONCE -> {
                CompletionStage<RecordMetadata> result = kafkaBridgeProducer.send(record);
                // wait for the result of the send operation
                result.whenComplete((metadata, error) -> {
                    if (error != null) {
                        LOGGER.error("Error sending message to Kafka: ", error);
                    } else {
                        LOGGER.info("Message sent to Kafka on topic {} with offset {}", metadata.topic(), metadata.offset());
                        // send PUBACK message to the client
                        sendPubAckMessage(ctx, publishMessage.variableHeader().packetId());
                    }
                });
            }
            case EXACTLY_ONCE -> LOGGER.warn("QoS level EXACTLY_ONCE is not supported");
            default -> throw new IllegalArgumentException("QoS level " + qos + "not supported");
        }
    }
}
