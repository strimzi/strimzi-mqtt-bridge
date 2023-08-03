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
import io.strimzi.kafka.bridge.mqtt.kafka.KafkaBridgeProducer;
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
    private final KafkaBridgeProducer kafkaBridgeProducer;

    private ChannelFuture channelFuture;

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
        this.kafkaBridgeProducer = new KafkaBridgeProducer(config.getKafkaConfig());
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.group(masterGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new MqttServerInitializer(this.kafkaBridgeProducer))
                .childOption(option, true);
    }

    /**
     * Start the server.
     */
    public void start() throws InterruptedException {
        // bind the Netty server and wait synchronously
        this.channelFuture = this.serverBootstrap.bind(this.mqttConfig.getHost(), this.mqttConfig.getPort()).sync();
    }

    /**
     * Stop the server.
     */
    public void stop() throws InterruptedException {
        logger.info("Shutting down Netty server...");
        this.channelFuture.channel().close().sync();
        this.channelFuture.channel().closeFuture().sync();
        this.masterGroup.shutdownGracefully().sync();
        this.workerGroup.shutdownGracefully().sync();
        logger.info("Netty server shut down");

        logger.info("Closing Kafka producers...");
        this.kafkaBridgeProducer.close();
        logger.info("Kafka producers closed");
    }
}
