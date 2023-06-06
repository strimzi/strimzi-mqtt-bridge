package io.strimzi.kafka.bridge.mqtt;

import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.strimzi.kafka.bridge.mqtt.core.MqttServer;
import io.strimzi.kafka.bridge.mqtt.core.MqttServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    static final int PORT = Integer.parseInt(System.getProperty("port", "1883"));

    public static void main(String[] args) {
        try {
            NioEventLoopGroup masterGroup = new NioEventLoopGroup();
            NioEventLoopGroup workerGroup = new NioEventLoopGroup();
            MqttServer server = new MqttServer(PORT, masterGroup, workerGroup, ChannelOption.SO_KEEPALIVE);
            server.start();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
