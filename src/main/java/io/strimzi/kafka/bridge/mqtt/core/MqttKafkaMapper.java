package io.strimzi.kafka.bridge.mqtt.core;


import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.strimzi.kafka.bridge.mqtt.utils.MappingRunnable;

/**
 * Represent the Mqtt to Kafka Mapper component.
 *
 */
public class MqttKafkaMapper {
    private static MqttKafkaMapper instance;

    /**
     * Private constructor
     *
     */
    private MqttKafkaMapper() {
    }


    /**
     * Get the instance of this class.
     *
     * @return MqttKafkaMapper instance.
     */
    public static synchronized MqttKafkaMapper getInstance() {
        if (instance == null) {
            instance = new MqttKafkaMapper();
        }
        return instance;
    }

    /**
     * Map the MqttPublishMessage to kafka record.
     *
     * @param ctx ChannelHandlerContext instance.
     * @param mqttMessage MqttPublishMessage to be mapped to Kafka.
     * @throws InterruptedException
     * @see MqttPublishMessage
     */
    public void map(ChannelHandlerContext ctx, MqttPublishMessage mqttMessage) throws InterruptedException {
        // Mapping logic implementation
        MappingRunnable runnable = new MappingRunnable(ctx, mqttMessage);
        Thread thread = new Thread(runnable);
        thread.start();
    }
}
