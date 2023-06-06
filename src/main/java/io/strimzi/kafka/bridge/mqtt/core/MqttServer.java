package io.strimzi.kafka.bridge.mqtt.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Represents the MqttServer component.
 */
public class MqttServer {
    private static final Logger logger = LoggerFactory.getLogger(MqttServer.class);

    private final EventLoopGroup masterGroup;
    private final EventLoopGroup workerGroup;
    private final int port;
    private final ServerBootstrap serverBootstrap;

    /**
     * This helper class help us add necessary Netty pipelines handlers. <br>
     * During the {@link #initChannel(SocketChannel)}, we use MqttDecoder() and MqttEncoder to decode and encode Mqtt messages respectively. <br>
     */
    private static class MqttServerInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) {
            ch.pipeline().addLast("decoder", new MqttDecoder());
            ch.pipeline().addLast("encoder", MqttEncoder.INSTANCE);
            ch.pipeline().addLast("handler", new MqttServerHandler());
        }
    }

    /**
     * Constructor
     *
     * @param port an integer that represents the port the server should be bound to.
     * @param masterGroup EventLoopGroup instance for handle incoming connections.
     * @param workerGroup EventLoopGroup instance for processing I/O.
     * @param option ChannelOption<Boolean> instance which allows to configure various channel options, such as SO_KEEPALIVE, SO_BACKLOG and etc.
     * @see ChannelOption
     */
    public MqttServer(int port, EventLoopGroup masterGroup, EventLoopGroup workerGroup, ChannelOption<Boolean> option) {
        this.masterGroup =  masterGroup;
        this.workerGroup = workerGroup;
        this.port = port;
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.group(masterGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new MqttServer.MqttServerInitializer())
                .childOption(option, true);
    }

    /**
     * Start the server.
     */
    public void start() {
        try {
            ChannelFuture channelFuture = this.serverBootstrap.bind(this.port).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e){
            logger.error(e.getMessage());
        } finally {
            this.stop();
        }
    }

    /**
     * Stop the server.
     */
    public void stop(){
        this.masterGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }
}
