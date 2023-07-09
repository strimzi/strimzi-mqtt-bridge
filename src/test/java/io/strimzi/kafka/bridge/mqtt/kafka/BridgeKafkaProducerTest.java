/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link BridgeKafkaProducer}
 */
public class BridgeKafkaProducerTest {

    /**
     * Test the {@link BridgeKafkaProducer#send(ProducerRecord)} method
     */
    @Test
    public void testProducerNoAck() {
        // mock the producer
        BridgeKafkaProducer producer = mock(BridgeKafkaProducer.class);

        String kafkaTopic = "test-topic";
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(kafkaTopic, "test".getBytes());

        // simulate the send method. First no ack, then ack
        when(producer.send(any(ProducerRecord.class)))
                .thenReturn(any()) // no ack
                .thenAnswer(invocation -> {
                    ProducerRecord<String, byte[]> r = invocation.getArgument(0);

                    assertThat("Topic is correct",
                            r.topic(), is(kafkaTopic));

                    assertThat("Value is correct",
                            r.value(), is("test".getBytes()));

                    CompletableFuture<RecordMetadata> promise = new CompletableFuture<>();

                    promise.complete(new RecordMetadata(new TopicPartition(r.topic(), 2), 234L, 0, 1000, 0L, 0, "test".getBytes().size()));
                    return promise;
                });

        // send the record and do not wait for ack
        producer.send(record);

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

        });
    }
}
