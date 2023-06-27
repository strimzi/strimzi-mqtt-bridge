[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Twitter Follow](https://img.shields.io/twitter/follow/strimziio?style=social)](https://twitter.com/strimziio)

# MQTT bridge for Apache Kafka®

This project provides a software component which acts as a bridge between [MQTT 3.1.1](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html) and an [Apache Kafka®](https://kafka.apache.org/) cluster.

## Running the bridge

### Locally

Clone the repository and update your dependencies using maven.
Afterward, edit the `config/application.properties` file which contains the configuration. And provide a valid [topic mapping rules file]().

Once your configuration is ready, start the bridge using:

```bash
<application> --config-file config/application.properties --mapping-rules=path/to/mapping-rules
```

You can find more information about the topic mapping rules in the MQTT Bridge.

### Running Tests
```bash
mvn clean test
```

## MQTT bridge documentation

### 1. MQTT Bridge Overview
You can use the MQTT bridge to take advantage of the benefits of both MQTT and Kafka in data processing and IoT system scenarios.

#### 1.1 General overview.

#### 1.2 How it works

The MQTT Bridge is a netty-based MQTT server that is responsible for handling all the MQTT client connections and connections management and is responsible for all MQTT-based operations like QoS, error handling, and other capabilities Netty offers.

The MQTT Bridge provides two interfaces to integrate MQTT and Apache Kafka. The mapper maps incoming MQTT topics to Kafka topics. Once this mapping is completed, the MQTT Bridge will transform the incoming MQTT payload to a valid Kafka record and then send to the appropriate Kafka topic through the bridge kafka Producer.

#### 1.2.1 The MqttKafkaMapper
This interface is responsible for mapping incoming MQTT topic to Kafka topic. How this mapping is should be made must be defined in the `Topic Mapping Rules(TOMAR)`.

#### 1.2.2 Topic Mapping Rules(TOMAR)

The TOMAR is a set of patterns the user provides, to which the MQTT Bridge will decide which Kafka topic to produce a message. 
A Mapping Rule is a model that contains an `MQTT topic pattern` and a `Kafka topic template`. All the incoming MQTT message's topic should match an `MQTT topic pattern` in the `TOMAR` so that the bridge knows in which Kafka topic to produce this message. This Kafka topic is defined by a template, `kafka Topic template`. However, if the incoming MQTT message's topic does not match any pattern in the `TOMAR`, the Bridge has default Kafka topic where the incoming message will be mapped to.

##### 1.2.2.1 Usage/Examples
A valid TOMAR is a JSON file that contains an array of mapping rules. 
Each mapping rule is a JSON object that contains two properties: `mqttTopic` and `kafkaTopic`. The following is an example of a valid TOMAR:
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
Let's go through each rule:

1. MQTT Topic: "building/{building}/room/{room}/#" -> Kafka Topic: "building_{building}room{room}"
    This rule maps MQTT topics of the form "building/{building}/room/{room}/#" to the Kafka topic "building_{building}room{room}". The wildcard "#" represents one or more levels in the topic hierarchy. For example, if the MQTT topic is "building/A/room/1/floor/2", it will be mapped to the Kafka topic "building_A_room_1_floor_2".

2. MQTT Topic: "sensors/#" -> Kafka Topic: "sensor_others"

    This rule maps any MQTT topic starting with "sensors/" followed by any number of levels in the hierarchy to the Kafka topic "sensor_others". The "#" wildcard matches any number of levels. For example, if the MQTT topic is "sensors/temperature/living-room", it will be mapped to the Kafka topic "sensor_others".

3. MQTT Topic: "sensors/+/data" -> Kafka Topic: "sensor_data"

    This rule maps MQTT topics of the form "sensors/+/data" to the Kafka topic "sensor_data". The "+" wildcard matches a single level in the topic hierarchy. For example, if the MQTT topic is "sensors/temperature/data", it will be mapped to the Kafka topic "sensor_data".

These mapping rules demonstrate how different MQTT topics can be mapped to corresponding Kafka topics based on predefined patterns. The MQTT Bridge uses these rules to map MQTT topics to Kafka topics.
The order in which the rules are defined is important. The MQTT Bridge will use the first rule that matches the MQTT topic. For example, if the MQTT topic is "sensors/temperature/data", 
it will be mapped to the Kafka topic "sensor_data" because the third rule matches the MQTT topic. If the third rule was not defined, the MQTT Bridge would use the second rule to map the MQTT topic to the Kafka topic "sensor_others".

### 2. MQTT Bridge Configuration
The user can configure the MQTT Bridge using the `application.properties` file. This section describes the configuration properties that can be used to configure the MQTT Bridge.
The MQTT bridge can be configured using the appropriate prefix. Example:
- `bridge.` is the prefix used for general configuration of the Bridge.
- `mqtt.` is the prefix used for MQTT configuration of the Bridge.
- `kafka.` is the prefix used for Kafka configuration of the Bridge.
### 2.1 Procedure to configure the MQTT Bridge
1. Find the `application.properties` file in the `config` directory.
2. Edit the file to configure the MQTT Bridge.
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
3. Save the file.

The following table describes the configuration properties that can be used to configure the MQTT Bridge.

| Setting                        | Description                                     | E.g. Value              |
| ------------------------------ | ----------------------------------------------- |-------------------------|
| bridge.id                      | ID of the bridge                                | my-bridge               |
| mqtt.server.host               | Host address of the MQTT server                 | localhost(default)      |
| mqtt.server.port               | Port number of the MQTT server                  | 1883(default)           |
| kafka.bootstrap.servers        | Bootstrap servers for Apache Kafka              | localhost:9092(default) |

Other than the above properties, the user can also configure the bridge using environment variables.

## License

Strimzi Kafka Bridge is licensed under the [Apache License](./LICENSE), Version 2.0