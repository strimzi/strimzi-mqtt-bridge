package io.strimzi.kafka.bridge.mqtt.core;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;

/**
 * Represents a SimpleChannelInboundHandler. The MqttServerHandler is responsible for: <br>
 * - listen to client connections;<br>
 * - listen to incoming messages; <br>
 *
 * @see io.netty.channel.SimpleChannelInboundHandler
 */
public class MqttServerHandler extends SimpleChannelInboundHandler<Object> {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.printf("Client " + ctx.channel().remoteAddress() +  " is trying to connect\n");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            MqttMessageType messageType = ((MqttMessage) msg).fixedHeader().messageType();

            if (messageType == MqttMessageType.CONNECT) {
                handleConnectMessage(ctx);
            } else if (messageType == MqttMessageType.PUBLISH) {
                handlePublishMessage(ctx, (MqttPublishMessage) msg);
            } else {
                // Handle other MQTT message types as needed
                System.out.printf(messageType.name()+"\n");
            }
        } catch (Exception e){
            System.out.printf(e.getMessage());
            ctx.close();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

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

        System.out.printf("Connected\n");
        ctx.writeAndFlush(connAckMessage);
    }

    /**
     * Handle the case when a client sent a MQTT PUBLISH message type.
     *
     * @param ctx ChannelHandlerContext instance
     * @param publishMessage represents a MqttPublishMessage
     * @throws InterruptedException
     */
    private void handlePublishMessage(ChannelHandlerContext ctx, MqttPublishMessage publishMessage) throws InterruptedException {
        MqttKafkaMapper.getInstance().map(ctx, publishMessage);
    }
}