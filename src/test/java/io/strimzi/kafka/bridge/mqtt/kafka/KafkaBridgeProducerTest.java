/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link KafkaBridgeProducer}
 */
public class KafkaBridgeProducerTest {

    /**
     * Test the {@link KafkaBridgeProducer#send(ProducerRecord)}} method
     */
    @Test
    public void testSend() {
        // mock the producer
        KafkaBridgeProducer producer = mock(KafkaBridgeProducer.class);

        String kafkaTopic = "test-topic";
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(kafkaTopic, "test".getBytes());

        // simulate the send method with ack
        when(producer.send(any(ProducerRecord.class)))
                .thenAnswer(invocation -> {
                    ProducerRecord<String, byte[]> r = invocation.getArgument(0);

                    assertThat("Topic is correct",
                            r.topic(), is(kafkaTopic));

                    assertThat("Value is correct",
                            r.value(), is("test".getBytes()));

                    CompletableFuture<RecordMetadata> promise = new CompletableFuture<>();

                    promise.complete(new RecordMetadata(new TopicPartition(r.topic(), 2), 234L, 0, 1000, 0L, 0, "test".getBytes().length));
                    return promise;
                });

        // send the record and wait for ack
        producer.send(record).thenAccept(metadata -> {

            // should have the correct partition
            assertThat("Partition is correct",
                    metadata.partition(), is(2));

            // should have the correct offset
            assertThat("Offset is correct",
                    metadata.offset(), is(234L));

            // should have the correct timestamp
            assertThat("Timestamp is correct",
                    metadata.timestamp(), is(1000L));

            // should have the correct serialized key size
            assertThat("Serialized key size is correct",
                    metadata.serializedKeySize(), is(0));

            // should have the correct serialized value size
            assertThat("Serialized value size is correct",
                    metadata.serializedValueSize(), is("test".getBytes().length));
        });
    }
}
