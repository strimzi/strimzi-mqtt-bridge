[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Twitter Follow](https://img.shields.io/twitter/follow/strimziio?style=social)](https://twitter.com/strimziio)

# MQTT bridge for Apache Kafka®

This project provides a software component which acts as a bridge between [MQTT 3.1.1](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html) and an [Apache Kafka®](https://kafka.apache.org/) cluster.


## MQTT Bridge Overview

### How it works

To enable a seamless integration between MQTT and Kafka, the MQTT Bridge provides a way to map MQTT topics to Kafka topics. 
This mapping is done using a set of predefined patterns and Kafka topic templates.
The MQTT Bridge uses these patterns to map MQTT topics to Kafka topics. 
As a part of the bridge, a Kafka producer will be responsible for producing messages from the MQTT clients to the Kafka Cluster.

### Topic Mapping Rules (ToMaR)

The ToMaR is a set of patterns the user provides defining how the MQTT Bridge maps MQTT topic names to Kafka topic names.
A Mapping Rule is a model that contains an `MQTT topic pattern`, a `Kafka topic template`, and optionally a `Kafka record key`.
All the incoming MQTT message's topic should match an `MQTT topic pattern` in the ToMaR so that the bridge knows in which Kafka topic to produce this message.
This Kafka topic is defined by a template, `Kafka Topic template`.
However, if the incoming MQTT message's topic does not match any pattern in the ToMaR, the Bridge has a default Kafka topic where the incoming message will be mapped to, known as `messages_default`.

The optional `Kafka record key` is used to define the key of the Kafka record that will be produced to the Kafka topic.
It is defined by a template as well, and its default value is `null`.
A valid ToMaR is a JSON file that contains an array of mapping rules.
Each mapping rule is a JSON object that contains two properties: `mqttTopic` and `kafkaTopic`.
The following is an example of a valid ToMaR:

```json
[
  {
     "mqttTopic": "building/(\\w+)/room/(\\d{1,4}).*",
     "kafkaTopic": "building_$1",
     "kafkaKey": "room_$2"
  },
  {
    "mqttTopic": "sensors/([^/]+)/data",
    "kafkaTopic": "sensor_data"
  },
  {
     "mqttTopic": "sensors.*",
     "kafkaTopic": "sensor_others"
  },
  {
    "mqttTopic": "devices/([^/]+)/data/(\b(all|new|old)\b)",
     "kafkaTopic": "device_$1_data",
     "kafkaKey": "device_$2"
  }
]
```

The expression `.*` is used to represent the wildcard `#`, which in turn represents one or more levels in the MQTT topic hierarchy.
The expression `([^/]+)` is used to represent the wildcard `+`, which in turn represents a single level in the MQTT topic hierarchy.
It worth's mentioning that it is the user's responsibility to adhere to the [MQTT 3.1.1 naming conventions](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718106) when defining the MQTT topic patterns.

Placeholders in the `KafkaTopic` and `kafkaKey` templates are defined using the `$` character followed by a number. 
The number represents the index of the capturing group in the `mqttTopic` pattern.
The MQTT Bridge uses the capturing groups in the `mqttTopic` pattern to positionally extract the values that will be used to replace the placeholders in the `KafkaTopic` and `kafkaKey` templates.

Let's go through each rule in the above example to understand how the MQTT Bridge uses these rules to map MQTT topics to Kafka topics:

1. MQTT Topic: `building/(\\w+)/room/(\\d{1,4}).*` -> Kafka Topic: `building_$1` with Kafka Key: `room_$2`

   This rule maps MQTT topics of the form `building/{some word}/room/{some number with 4 digits}/#` to the Kafka topic `building_$1`.
   For example, if the MQTT topic is `building/A/room/1003/floor/2` it will be mapped to the Kafka topic `building_A` with the key `room_1003`.

2. MQTT Topic: `sensors/([^/]+)/data` -> Kafka Topic: `sensor_data` with Kafka Key: `null`

   This rule maps MQTT topics of the form `sensors/+/data` to the Kafka topic `sensor_data`.
   For example, if the MQTT topic is `sensors/temperature/data`, it will be mapped to the Kafka topic `sensor_data`.
   Because the `KafkaKey` is not defined, the key of the Kafka record will be `null`.

3. MQTT Topic: `sensors.*` -> Kafka Topic: `sensor_others` with Kafka Key: `null`

   This rule maps any MQTT topic starting with `sensors/` followed by any number of levels in the hierarchy to the Kafka topic `sensor_others`.
   For example, if the MQTT topic is `sensors/temperature/living-room`, it will be mapped to the Kafka topic `sensor_others` with the key `sensor`.
   Because the `KafkaKey` is not defined, the key of the Kafka record will be `null`.

4. MQTT Topic: `devices/([^/]+)/data/(\b(all|new|old)\b)` -> Kafka Topic: `device_$1_data` with Kafka Key: `device_$2`

   This rule maps MQTT topics of the form `devices/{some word}/data/{either all, new or old}` to the Kafka topic `device_$1_data`.
   For example, if the MQTT topic is `devices/thermostat/data/all`, it will be mapped to the Kafka topic `device_thermostat_data` with the key `device_all`.
   This example also shows the advantage of using regex in the `mqttTopic` pattern. The last capturing group in the `mqttTopic` should be a word boundary `\b` followed by either `all`, `new` or `old` and anything else will not match the pattern.

The order in which the rules are defined is important.
The MQTT Bridge will use the first rule that matches the MQTT topic.
For example, if the MQTT topic is `sensors/temperature/data`, it will be mapped to the Kafka topic `sensor_data` because `sensors/([^/]+)/data` matches the MQTT topic before `sensors/#`.
If we swap the positions of the rules, the MQTT Bridge would use the `sensors.*` to map the MQTT topic to the Kafka  topic `sensor_others`.

### MQTT Bridge Configuration

The user can configure the MQTT Bridge using an `application.properties` file.
This section describes the configuration properties that can be used to configure the MQTT Bridge. 
The MQTT bridge can be configured using the appropriate prefix. Example:

- `bridge.` is the prefix used for general configuration of the Bridge.
- `mqtt.` is the prefix used for MQTT configuration of the Bridge.
- `kafka.` is the prefix used for Kafka configurations of the Bridge.

A valid configuration file should look like this:

```properties
    # Bridge configuration
    bridge.id=my-bridge
    # MQTT configuration
    mqtt.server.host=localhost
    mqtt.server.port=1883
    # Kafka configuration
    kafka.bootstrap.servers=localhost:9092
   ```

The following table describes the configuration properties defined above.

| Setting                 | Description                        | Default        |
|-------------------------|------------------------------------|----------------|
| bridge.id               | ID of the bridge                   | my-bridge      |
| mqtt.server.host        | Host address of the MQTT server    | localhost      |
| mqtt.server.port        | Port number of the MQTT server     | 1883           |
| kafka.bootstrap.servers | Bootstrap servers for Apache Kafka | localhost:9092 |

Other than the above properties, the user can also configure the bridge using environment variables.

## License

Strimzi Kafka Bridge is licensed under the [Apache License](./LICENSE), Version 2.0