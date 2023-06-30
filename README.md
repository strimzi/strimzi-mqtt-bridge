[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Twitter Follow](https://img.shields.io/twitter/follow/strimziio?style=social)](https://twitter.com/strimziio)

# MQTT bridge for Apache Kafka®

This project provides a software component which acts as a bridge between [MQTT 3.1.1](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html) and an [Apache Kafka®](https://kafka.apache.org/) cluster.

## MQTT bridge documentation

### 1. MQTT Bridge Overview

#### 1.1 How it works

To enable a seamless integration between MQTT and Kafka, the MQTT Bridge provides a way to map MQTT topics to Kafka topics. 
This mapping is done using a set of predefined patterns and kafka topic templates. The MQTT Bridge uses these patterns to map MQTT topics to Kafka topics. 
As a part of the bridge, a Kafka producer will be responsible for producing messages from the MQTT clients to the Kafka Cluster.

#### 1.2. Topic Mapping Rules(TOMAR)

The TOMAR is a set of patterns the user provides defining how the MQTT Bridge maps MQTT topic names to Kafka topic names.
A Mapping Rule is a model that contains an `MQTT topic pattern` and a `Kafka topic template`. 
All the incoming MQTT message's topic should match an `MQTT topic pattern` in the `TOMAR` so that the bridge knows in which Kafka topic to produce this message. This Kafka topic is defined by a template, `kafka Topic template`. However, if the incoming MQTT message's topic does not match any pattern in the `TOMAR`, the Bridge has a default Kafka topic where the incoming message will be mapped to, know as `messages_default`.

A valid TOMAR is a JSON file that contains an array of mapping rules. Each mapping rule is a JSON object that contains two properties: `mqttTopic` and `kafkaTopic`. The following is an example of a valid TOMAR:

```json
[
  {
    "mqttTopic": "building/{building}/room/{room}/#",
    "kafkaTopic": "building_{building}_room_{room}"
  },
  {
    "mqttTopic": "sensors/#",
    "kafkaTopic": "sensor_others"
  },
  {
    "mqttTopic": "sensors/+/data",
    "kafkaTopic": "sensor_data"
  }
]
```

The wildcard "#" represents one or more levels in the MQTT topic hierarchy. The wildcard "+" represents a single level in the MQTT topic hierarchy.
It worth's mentioning that it is the user's responsibility to adhere to the [MQTT 3.1.1 naming conventions](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718106) when defining the MQTT topic patterns.

The parts with curly braces in the MQTT topic pattern are called `placeholders`. The MQTT Bridge will extract the values of these placeholders from the MQTT topic and use them to construct the Kafka topic name using the `kafkaTopic` template.

Let's go through each rule to understand how the MQTT Bridge uses these rules to map MQTT topics to Kafka topics:

1. MQTT Topic: "building/{building}/room/{room}/#" -> Kafka Topic: "building_{building}room{room}"

    This rule maps MQTT topics of the form "building/{building}/room/{room}/#" to the Kafka topic "building_{building}room{room}". For example, if the MQTT topic is "building/A/room/1/floor/2", it will be mapped to the Kafka topic "building_A_room_1_floor_2".

2. MQTT Topic: "sensors/#" -> Kafka Topic: "sensor_others"

    This rule maps any MQTT topic starting with "sensors/" followed by any number of levels in the hierarchy to the Kafka topic "sensor_others". For example, if the MQTT topic is "sensors/temperature/living-room",  it will be mapped to the Kafka topic "sensor_others".

3. MQTT Topic: "sensors/+/data" -> Kafka Topic: "sensor_data"

    This rule maps MQTT topics of the form "sensors/+/data" to the Kafka topic "sensor_data". For example, if the MQTT topic is "sensors/temperature/data", it will be mapped to the Kafka topic "sensor_data".

The order in which the rules are defined is important. The MQTT Bridge will use the first rule that matches the MQTT topic. For example, if the MQTT topic is "sensors/temperature/data", it will be mapped to the Kafka topic "sensor_data" because the third rule matches the MQTT topic. If the third rule was not defined, the MQTT Bridge would use the second rule to map the MQTT topic to the Kafka topic "sensor_others".

### 2. MQTT Bridge Configuration

The user can configure the MQTT Bridge using an `application.properties` file.
This section describes the configuration properties that can be used to configure the MQTT Bridge. 
The MQTT bridge can be configured using the appropriate prefix.
Example:

- `bridge.` is the prefix used for general configuration of the Bridge.
- `mqtt.` is the prefix used for MQTT configuration of the Bridge.
- `kafka.` is the prefix used for Kafka producer configurations of the Bridge.

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