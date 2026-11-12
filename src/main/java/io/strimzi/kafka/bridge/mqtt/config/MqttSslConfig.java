/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public static final String MQTT_SSL_NAMED_GROUPS = MQTT_SSL_CONFIG_PREFIX + "named.groups";

    public static final boolean DEFAULT_MQTT_SSL_ENABLE = false;

    public static final String DEFAULT_MQTT_SSL_ENABLED_PROTOCOLS = "TLSv1.2,TLSv1.3";

    /** Sensitive MQTT config keys for inline PEM values */
    static final Set<String> MQTT_SENSITIVE_CONFIG_KEYS = Set.of(
            "ssl.key",
            "ssl.certificate"
    );

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
        return (String) this.config.get(MQTT_SSL_CERTIFICATE);
    }

    /**
     * Gets the file location of the server certificate in PEM format.
     *
     * @return the certificate file path, or {@code null} if not set
     */
    public String getCertificateLocation() {
        return (String) this.config.get(MQTT_SSL_CERTIFICATE_LOCATION);
    }

    /**
     * Gets the inline server private key in PEM format.
     * Inline values take precedence over {@link #getKeyLocation()}.
     *
     * @return the private key PEM, or {@code null} if not set
     */
    public String getKey() {
        return (String) this.config.get(MQTT_SSL_KEY);
    }

    /**
     * Gets the file location of the server private key in PEM format.
     *
     * @return the private key file path, or {@code null} if not set
     */
    public String getKeyLocation() {
        return (String) this.config.get(MQTT_SSL_KEY_LOCATION);
    }

    /**
     * Gets the enabled TLS protocol versions.
     * If not set, {@link #DEFAULT_MQTT_SSL_ENABLED_PROTOCOLS} is used.
     *
     * @return the enabled TLS protocols
     */
    public Set<String> getEnabledProtocols() {
        return Arrays.stream(((String) this.config.getOrDefault(MQTT_SSL_ENABLED_PROTOCOLS, DEFAULT_MQTT_SSL_ENABLED_PROTOCOLS)).split(",")).collect(Collectors.toSet());
    }

    /**
     * Gets the enabled TLS cipher suites.
     * If not set, {@code null} is returned and the JDK SSL/TLS engine defaults are used.
     *
     * @return the enabled cipher suites, or {@code null} to use the JDK defaults
     */
    public Set<String> getEnabledCipherSuites() {
        String cipherSuites = (String) this.config.get(MQTT_SSL_ENABLED_CIPHER_SUITES);
        if (cipherSuites != null) {
            return Arrays.stream(cipherSuites.split(",")).collect(Collectors.toSet());
        } else {
            return null;
        }
    }

    /**
     * Gets the enabled TLS named groups.
     * If not set, {@code null} is returned and the JDK SSL/TLS engine defaults are used.
     *
     * @return the enabled named groups, or {@code null} to use the JDK defaults
     */
    public List<String> getNamedGroups() {
        String namedGroups = (String) this.config.getOrDefault(MQTT_SSL_NAMED_GROUPS, null);
        if (namedGroups != null) {
            return Arrays.stream(namedGroups.split(",")).toList();
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "MqttSslConfig(" +
                "config=" + this.hideSecrets(MQTT_SENSITIVE_CONFIG_KEYS) +
                ")";
    }
}
