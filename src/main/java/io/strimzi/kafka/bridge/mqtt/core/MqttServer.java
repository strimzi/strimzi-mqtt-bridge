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
public class MqttServer implements Liveness, Readiness {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttServer.class);
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
                .childHandler(new MqttServerInitializer(this.kafkaBridgeProducer, config.getBridgeDefaultTopic()))
                .childOption(option, true);
    }

    /**
     * Start the server.
     */
    public void start() {
        try {
            // bind the Netty server and wait synchronously
            this.channelFuture = this.serverBootstrap.bind(this.mqttConfig.getHost(), this.mqttConfig.getPort()).sync();
        } catch (Exception e) {
            LOGGER.error("Failed to start the MQTT server", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Stop the server.
     */
    public void stop() {
        try {
            LOGGER.info("Shutting down Netty server...");
            this.channelFuture.channel().close().sync();
            this.channelFuture.channel().closeFuture().sync();
            this.masterGroup.shutdownGracefully().sync();
            this.workerGroup.shutdownGracefully().sync();
            LOGGER.info("Netty server shut down");

            LOGGER.info("Closing Kafka producers...");
            this.kafkaBridgeProducer.close();
            LOGGER.info("Kafka producers closed");
        } catch (Exception e) {
            LOGGER.error("Failed to stop the MQTT server", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isAlive() {
        return !this.workerGroup.isTerminated() && !this.masterGroup.isTerminated();
    }

    @Override
    public boolean isReady() {
        return !this.workerGroup.isTerminated() && !this.masterGroup.isTerminated();
    }
}
