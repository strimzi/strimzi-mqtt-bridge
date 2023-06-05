package io.strimiz.strimzi_mqtt_bridge;

import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.strimiz.strimzi_mqtt_bridge.core.MqttServer;

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
