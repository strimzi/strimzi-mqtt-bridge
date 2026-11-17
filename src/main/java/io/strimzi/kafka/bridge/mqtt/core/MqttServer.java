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
import io.strimzi.kafka.bridge.mqtt.MqttSslContextProvider;
import io.strimzi.kafka.bridge.mqtt.config.BridgeConfig;
import io.strimzi.kafka.bridge.mqtt.config.MqttConfig;
import io.strimzi.kafka.bridge.mqtt.kafka.KafkaBridgeProducer;
import io.strimzi.kafka.bridge.mqtt.mapper.MappingRule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Represents the MqttServer component.
 */
public class MqttServer implements Liveness, Readiness {
    private static final Logger LOGGER = LogManager.getLogger(MqttServer.class);
    private final EventLoopGroup masterGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap serverBootstrap;
    private final MqttConfig mqttConfig;
    private final KafkaBridgeProducer kafkaBridgeProducer;
    private ChannelFuture channelFuture;

    /**
     * Creates a MqttServer instance, loading the SSL context first (when SSL/TLS is enabled)
     * so that a failure happens before any other resource, like the Kafka producers, is created.
     *
     * @param config       MqttConfig instance with all configuration needed to run the server.
     * @param masterGroup  EventLoopGroup instance for handle incoming connections.
     * @param workerGroup  EventLoopGroup instance for processing I/O.
     * @param option       ChannelOption<Boolean> instance which allows to configure various channel options, such as SO_KEEPALIVE, SO_BACKLOG etc.
     * @param mappingRules the list of topic mapping rules to apply
     * @return the MqttServer instance
     * @throws SslContextLoadException if the SSL configuration is invalid or the SSL context cannot be loaded
     * @see BridgeConfig
     * @see ChannelOption
     */
    public static MqttServer create(BridgeConfig config, EventLoopGroup masterGroup, EventLoopGroup workerGroup, ChannelOption<Boolean> option, List<MappingRule> mappingRules) {
        MqttSslContextProvider sslContextProvider = MqttSslContextProvider.load(config.getMqttConfig().getSslConfig());
        return new MqttServer(config, masterGroup, workerGroup, option, mappingRules, sslContextProvider);
    }

    /**
     * Constructor
     *
     * @param config       MqttConfig instance with all configuration needed to run the server.
     * @param masterGroup  EventLoopGroup instance for handle incoming connections.
     * @param workerGroup  EventLoopGroup instance for processing I/O.
     * @param option       ChannelOption<Boolean> instance which allows to configure various channel options, such as SO_KEEPALIVE, SO_BACKLOG etc.
     * @param mappingRules the list of topic mapping rules to apply
     * @param sslContextProvider the already loaded SSL context provider, or {@code null} when SSL/TLS is disabled
     * @see BridgeConfig
     * @see ChannelOption
     */
    MqttServer(BridgeConfig config, EventLoopGroup masterGroup, EventLoopGroup workerGroup, ChannelOption<Boolean> option, List<MappingRule> mappingRules, MqttSslContextProvider sslContextProvider) {
        this.masterGroup = masterGroup;
        this.workerGroup = workerGroup;
        this.mqttConfig = config.getMqttConfig();
        this.kafkaBridgeProducer = new KafkaBridgeProducer(config.getKafkaConfig());
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.group(masterGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new MqttServerInitializer(this.kafkaBridgeProducer, config, sslContextProvider, mappingRules))
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
            // channelFuture is null if the server was never started (or failed to bind)
            if (this.channelFuture != null) {
                this.channelFuture.channel().close().sync();
                this.channelFuture.channel().closeFuture().sync();
            }
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
