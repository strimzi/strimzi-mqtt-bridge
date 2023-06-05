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

public class MqttServer {

    private final EventLoopGroup masterGroup;
    private final EventLoopGroup workerGroup;
    private final int port;
    private final ServerBootstrap serverBootstrap;

    //define initializer class
    private static class MqttServerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) {
            ch.pipeline().addLast("decoder", new MqttDecoder());
            ch.pipeline().addLast("encoder", MqttEncoder.INSTANCE);
            ch.pipeline().addLast("handler", new MqttServerHandler());
        }
    }

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

    public void start() {
        try {
            ChannelFuture channelFuture = this.serverBootstrap.bind(this.port).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e){
            System.out.printf(e.getMessage());
        } finally {
            this.stop();
        }
    }

    public void stop(){
        this.masterGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }
}