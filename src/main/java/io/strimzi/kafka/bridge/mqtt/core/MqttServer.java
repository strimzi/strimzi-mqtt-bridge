/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.strimzi.kafka.bridge.mqtt.config.BridgeConfig;
import io.strimzi.kafka.bridge.mqtt.config.MqttConfig;
import io.strimzi.kafka.bridge.mqtt.kafka.BridgeKafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the MqttServer component.
 */
public class MqttServer {
    private static final Logger logger = LoggerFactory.getLogger(MqttServer.class);
    private final EventLoopGroup masterGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap serverBootstrap;
    private final MqttConfig mqttConfig;
    private final BridgeKafkaProducerService producerService;

    /**
     * Constructor
     *
     * @param config      MqttConfig instance with all configuration needed to run the server.
     * @param masterGroup EventLoopGroup instance for handle incoming connections.
     * @param workerGroup EventLoopGroup instance for processing I/O.
     * @param option      ChannelOption<Boolean> instance which allows to configure various channel options, such as SO_KEEPALIVE, SO_BACKLOG etc.
     * @see BridgeConfig
     * @see ChannelOption
     */
    public MqttServer(BridgeConfig config, EventLoopGroup masterGroup, EventLoopGroup workerGroup, ChannelOption<Boolean> option) {
        this.masterGroup = masterGroup;
        this.workerGroup = workerGroup;
        this.mqttConfig = config.getMqttConfig();
        this.producerService = new BridgeKafkaProducerService(config.getKafkaConfig());
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.group(masterGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new MqttServerInitializer(this.producerService))
                .childOption(option, true);
    }

    /**
     * Start the server.
     */
    public void start() {
        try {
            int port = this.mqttConfig.getPort();
            String host = this.mqttConfig.getHost();
            ChannelFuture channelFuture = this.serverBootstrap.bind(host, port).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("Error starting the MQTT server: ", e);
        } finally {
            this.stop();
        }
    }

    /**
     * Stop the server.
     */
    public void stop() {
        this.masterGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        logger.info("Closing Kafka producers...");
        this.producerService.close();
        logger.info("Kafka producers closed");
    }
}
