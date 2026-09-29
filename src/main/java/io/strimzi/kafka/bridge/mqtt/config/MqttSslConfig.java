/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents SSL/TLS configurations related to MQTT
 *
 * @see AbstractConfig
 */
public class MqttSslConfig extends AbstractConfig {

    // Prefix for all the specific configuration parameters for MQTT SSL/TLS in the properties file
    public static final String MQTT_SSL_CONFIG_PREFIX = MqttConfig.MQTT_CONFIG_PREFIX + "ssl.";

    public static final String MQTT_SSL_ENABLE = MQTT_SSL_CONFIG_PREFIX + "enable";

    public static final String MQTT_SSL_CERTIFICATE = MQTT_SSL_CONFIG_PREFIX + "certificate";

    public static final String MQTT_SSL_CERTIFICATE_LOCATION = MQTT_SSL_CONFIG_PREFIX + "certificate.location";

    public static final String MQTT_SSL_KEY = MQTT_SSL_CONFIG_PREFIX + "key";

    public static final String MQTT_SSL_KEY_LOCATION = MQTT_SSL_CONFIG_PREFIX + "key.location";

    public static final String MQTT_SSL_ENABLED_PROTOCOLS = MQTT_SSL_CONFIG_PREFIX + "enabled.protocols";

    public static final String MQTT_SSL_ENABLED_CIPHER_SUITES = MQTT_SSL_CONFIG_PREFIX + "enabled.cipher.suites";

    public static final boolean DEFAULT_MQTT_SSL_ENABLE = false;

    public static final List<String> DEFAULT_MQTT_SSL_ENABLED_PROTOCOLS = List.of("TLSv1.2", "TLSv1.3");

    /**
     * Constructor
     *
     * @param config configuration parameters map
     */
    public MqttSslConfig(Map<String, Object> config) {
        super(config);
    }

    /**
     * Build an MQTT SSL/TLS configuration object from a map of configuration parameters
     *
     * @param map configuration parameters map
     * @return a new instance of MqttSslConfig
     */
    public static MqttSslConfig fromMap(Map<String, Object> map) {
        return new MqttSslConfig(map.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(MQTT_SSL_CONFIG_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * Checks whether SSL/TLS is enabled for MQTT client connections.
     *
     * @return {@code true} if SSL/TLS is enabled, {@code false} otherwise
     */
    public boolean isEnabled() {
        return Boolean.parseBoolean(this.config.getOrDefault(MQTT_SSL_ENABLE, DEFAULT_MQTT_SSL_ENABLE).toString());
    }

    /**
     * Gets the inline server certificate in PEM format.
     * Inline values take precedence over {@link #getCertificateLocation()}.
     *
     * @return the certificate PEM, or {@code null} if not set
     */
    public String getCertificate() {
        return this.config.get(MQTT_SSL_CERTIFICATE) == null ? null : this.config.get(MQTT_SSL_CERTIFICATE).toString();
    }

    /**
     * Gets the file location of the server certificate in PEM format.
     *
     * @return the certificate file path, or {@code null} if not set
     */
    public String getCertificateLocation() {
        return this.config.get(MQTT_SSL_CERTIFICATE_LOCATION) == null ? null : this.config.get(MQTT_SSL_CERTIFICATE_LOCATION).toString();
    }

    /**
     * Gets the inline server private key in PEM format.
     * Inline values take precedence over {@link #getKeyLocation()}.
     *
     * @return the private key PEM, or {@code null} if not set
     */
    public String getKey() {
        return this.config.get(MQTT_SSL_KEY) == null ? null : this.config.get(MQTT_SSL_KEY).toString();
    }

    /**
     * Gets the file location of the server private key in PEM format.
     *
     * @return the private key file path, or {@code null} if not set
     */
    public String getKeyLocation() {
        return this.config.get(MQTT_SSL_KEY_LOCATION) == null ? null : this.config.get(MQTT_SSL_KEY_LOCATION).toString();
    }

    /**
     * Gets the enabled TLS protocol versions.
     * If not set, {@link #DEFAULT_MQTT_SSL_ENABLED_PROTOCOLS} is used.
     *
     * @return the enabled TLS protocols
     */
    public List<String> getEnabledProtocols() {
        Object value = this.config.get(MQTT_SSL_ENABLED_PROTOCOLS);
        if (value == null || value.toString().isBlank()) {
            return DEFAULT_MQTT_SSL_ENABLED_PROTOCOLS;
        }
        return parseCommaSeparated(value.toString());
    }

    /**
     * Gets the enabled TLS cipher suites.
     * If not set, an empty list is returned and the JDK SSL/TLS engine defaults are used.
     *
     * @return the enabled cipher suites, or an empty list to use the JDK defaults
     */
    public List<String> getEnabledCipherSuites() {
        Object value = this.config.get(MQTT_SSL_ENABLED_CIPHER_SUITES);
        if (value == null || value.toString().isBlank()) {
            return List.of();
        }
        return parseCommaSeparated(value.toString());
    }

    @Override
    public String toString() {
        Map<String, Object> configToString = this.hideSecrets();
        return "MqttSslConfig(" +
                "config=" + configToString +
                ")";
    }

    /**
     * Hides the inline private key by replacing the actual value with [hidden]
     *
     * @return updated configuration with the private key hidden
     */
    private Map<String, Object> hideSecrets() {
        Map<String, Object> configToString = new HashMap<>(this.config);
        if (configToString.containsKey(MQTT_SSL_KEY) || configToString.containsKey(MQTT_SSL_CERTIFICATE)) {
            configToString.put(MQTT_SSL_KEY, "[hidden]");
            configToString.put(MQTT_SSL_CERTIFICATE, "[hidden]");
        }
        return configToString;
    }

    private static List<String> parseCommaSeparated(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }
}
