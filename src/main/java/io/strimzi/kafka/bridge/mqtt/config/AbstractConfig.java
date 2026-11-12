/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.mqtt.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base abstract class for configurations related to protocols heads and Kafka
 */
public abstract class AbstractConfig {
    protected final Map<String, Object> config;

    /**
     * Constructor
     *
     * @param config configuration parameters map
     */
    public AbstractConfig(Map<String, Object> config) {
        this.config = config;
    }

    /**
     * @return configuration parameters map
     */
    public Map<String, Object> getConfig() {
        return this.config;
    }

    /**
     * Returns a copy of the configuration map with sensitive values replaced by {@code [hidden]}.
     * A key is considered sensitive when it ends with any of the provided sensitive config keys.
     *
     * @param sensitiveConfigKeys set of config keys that identify sensitive entries
     * @return sanitized copy of the configuration map
     */
    protected Map<String, Object> hideSecrets(Set<String> sensitiveConfigKeys) {
        Map<String, Object> sanitized = new HashMap<>(this.config);
        sanitized.entrySet().stream()
                .filter(e -> sensitiveConfigKeys.stream().anyMatch(e.getKey()::endsWith))
                .forEach(e -> e.setValue("[hidden]"));
        return sanitized;
    }
}
