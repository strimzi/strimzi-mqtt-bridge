package io.strimzi.kafka.bridge.mqtt.utils;


import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

import java.nio.charset.Charset;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a Runnable which will be used to run the mapping phase of the MqttKafkaMapper.
 *
 * @see io.strimzi.kafka.bridge.mqtt.core.MqttKafkaMapper
 */
public class MappingRunnable implements Runnable {
    private final MqttPublishMessage mqttMessage;
    private final ChannelHandlerContext ctx;
    private final Lock lock;


    /**
     * Constructor
     *
     * @param ctx represents a ChannelHandlerContext
     * @param mqttMessage represents a MqttPublishMessage
     */
    public MappingRunnable(ChannelHandlerContext ctx, MqttPublishMessage mqttMessage) {
        this.mqttMessage = mqttMessage;
        this.ctx = ctx;
        this.lock = new ReentrantLock();
    }

    @Override
    public void run() {
        lock.lock();
        try {
            System.out.println("Mapping");
            System.out.println("Topic: " + mqttMessage.variableHeader().topicName());
            System.out.println("Message: " + mqttMessage.payload().toString(Charset.defaultCharset()));

            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
