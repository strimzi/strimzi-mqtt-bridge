package io.strimzi.kafka.bridge.mqtt;

import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.strimzi.kafka.bridge.mqtt.core.MqttServer;

public class Main {
    static final int PORT = Integer.parseInt(System.getProperty("port", "1883"));

    public static void main(String[] args) {
        try {
            NioEventLoopGroup masterGroup = new NioEventLoopGroup();
            NioEventLoopGroup workerGroup = new NioEventLoopGroup();
            MqttServer server = new MqttServer(PORT, masterGroup, workerGroup, ChannelOption.SO_KEEPALIVE);
            server.start();
        } catch (Exception e){
            System.out.printf(e.getMessage());
        }
    }
}
