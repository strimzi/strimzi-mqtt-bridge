/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

/**
 * Serializer for ByteBuf
 */
public class ByteBufSerializer implements Serializer<ByteBuf> {
    @Override
    public byte[] serialize(String topic, ByteBuf data) {
        try {
            byte[] serializedData = new byte[data.readableBytes()];

            try (ByteBufInputStream inputStream = new ByteBufInputStream(data)) {
                inputStream.read(serializedData);
            }
            return serializedData;
        } catch (IOException e) {
            throw new SerializationException("Error serializing ByteBuf", e);
        }
    }
}
