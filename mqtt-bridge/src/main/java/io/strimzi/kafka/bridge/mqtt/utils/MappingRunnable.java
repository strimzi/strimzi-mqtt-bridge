package io.strimzi.kafka.bridge.mqtt.utils;


import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.strimzi.kafka.bridge.mqtt.core.MqttServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a Runnable which will be used to run the mapping phase of the MqttKafkaMapper.
 *
 * @see io.strimzi.kafka.bridge.mqtt.core.MqttKafkaMapper
 */
public class MappingRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MappingRunnable.class);
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
            logger.info("MAPPING");
            logger.info("Topic: {}", mqttMessage.variableHeader().topicName());
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
