/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link ConfigRetriever}.
 */
public class ConfigRetrieverTest {

    /**
     * Test if the application.properties file has all configuration parameters.
     */
    @Test
    public void testApplicationPropertiesFile() throws IOException {
        String filePath = Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getPath();
        Map<String, Object> config = ConfigRetriever.getConfig(filePath);
        BridgeConfig bridgeConfig = BridgeConfig.fromMap(config);

        // bridge config related tests
        assertThat("Bridge-ID should be 'my-bridge'",
                bridgeConfig.getBridgeID(), is("my-bridge"));
        assertThat("Bridge default topic should be 'bridge_topic'",
                bridgeConfig.getBridgeDefaultTopic(), is("bridge_topic"));

        // Mqtt server config related tests
        assertThat("There should be 2 related mqtt server config parameters",
                bridgeConfig.getMqttConfig().getConfig().size(), is(2));
        assertThat("Mqtt server host should be 'localhost'",
                bridgeConfig.getMqttConfig().getHost(), is("localhost"));
        assertThat("Mqtt server port should be '1883'",
                bridgeConfig.getMqttConfig().getPort(), is(1883));

        // Kafka server config related tests
        assertThat("There should be 1 related kafka config parameters",
                bridgeConfig.getKafkaConfig().getConfig().size(), is(1));
        assertThat("The address of the kafka bootstrap server should be 'localhost:9092'",
                bridgeConfig.getKafkaConfig().getConfig().get("bootstrap.servers"), is("localhost:9092"));
    }

    /**
     * Test if the application.properties not exists.
     */
    @Test
    public void testApplicationPropertiesFileNotExists()  {
        Exception fileNotFoundException = assertThrows(FileNotFoundException.class, () -> ConfigRetriever.getConfig("not-exists-application.properties"));

        assertThat("File not found exception should be thrown",
                fileNotFoundException.getMessage(), is("not-exists-application.properties (No such file or directory)"));
    }

    /**
     * Test environment variables override.
     */
    @Test
    public void testEnvironmentEnvOverride() throws IOException {
        String filePath = Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getPath();
        Map<String, String> envs = new java.util.HashMap<>(Map.of());
        // add all environment variables
        envs.putAll(System.getenv());

        // add new environment variable for bridge-id
        envs.put(BridgeConfig.BRIDGE_ID, "my-bridge-env");

        Map<String, Object> config = ConfigRetriever.getConfig(filePath, envs);
        BridgeConfig bridgeConfig = BridgeConfig.fromMap(config);

        assertThat("Bridge-ID should be 'my-bridge-env'",
                bridgeConfig.getBridgeID(), is("my-bridge-env"));
    }
}
