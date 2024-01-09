[![Build Status](https://dev.azure.com/cncf/strimzi/_apis/build/status%2Fmqtt-bridge%2Fmqtt-bridge?branchName=main)](https://dev.azure.com/cncf/strimzi/_build/latest?definitionId=59&branchName=main)
[![GitHub release](https://img.shields.io/github/release/strimzi/strimzi-mqtt-bridge.svg)](https://github.com/strimzi/strimzi-mqtt-bridge/releases/latest)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Twitter Follow](https://img.shields.io/twitter/follow/strimziio?style=social)](https://twitter.com/strimziio)

# MQTT bridge for Apache Kafka®

This project provides a software component which acts as a bridge between [MQTT 3.1.1](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html) and an [Apache Kafka®](https://kafka.apache.org/) cluster.
It enables the one-way communication from MQTT to Kafka, allowing MQTT clients to send data to an Apache Kafka cluster.
MQTT subscriptions to read data from the Apache Kafka brokers is out of scope.

## Running the bridge

### On bare-metal / VM

Download the ZIP or TAR.GZ file from the [GitHub release page](https://github.com/strimzi/strimzi-mqtt-bridge/releases) and unpack it.
Afterwards, edit the `config/application.properties` file which contains the configuration and the `config/topic-mapping-rules.json` with the topics' mapping rules. 
Once your configuration is ready, start the bridge using:

```shell
bin/mqtt_bridge_run.sh --config-file config/application.properties --mapping-rules config/topic-mapping-rules.json
```

### On Kubernetes and OpenShift

Download the ZIP or TAR.GZ file from the [GitHub release page](https://github.com/strimzi/strimzi-mqtt-bridge/releases) and unpack it.
The MQTT Bridge is deployed using a Kubernetes `Deployment`, and it is configured using a `ConfigMap`.
The `ConfigMap` contains the configuration and the topics' mapping rules files.
The files under the `install` directory are used to deploy the MQTT Bridge on Kubernetes.

To deploy the MQTT Bridge use the following command:

```shell
kubectl apply -f ./install
````

## Bridge Overview

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
However, if the incoming MQTT message's topic does not match any pattern in the ToMaR, the Bridge uses a default Kafka topic where the incoming message will be mapped to.
This Kafka topic is configurable but if it is not specified, the bridge uses the `messages_default` topic name. 

The optional `Kafka record key` is used to define the key of the Kafka record that will be produced to the Kafka topic.
It is defined by a template as well, and its default value is `null`.
A valid ToMaR is a JSON file that contains an array of mapping rules.
Each mapping rule is a JSON object that contains two mandatory properties, `mqttTopic`, `kafkaTopic`, and one optional `kafkaKey`.
The following is an example of a valid ToMaR:

```json
[
  {
    "mqttTopic": "building/(\\w+)/room/(\\d{1,4})/.*", 
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
  },
  {
    "mqttTopic": "locations/([^/]+)(?:\\/.*)?$", 
    "kafkaTopic": "locations", 
    "kafkaKey": "location_$1"
  }
]
```

* The expressions `.*` and `(?:\\/.*)?$` are used to represent the wildcard `#`, which in turn represents one or more levels in the MQTT topic hierarchy.
However, there are some cases that can lead to unexpected behavior when using the these wildcards interchangeably . 
Therefore, you should note the following:
  * You cannot use the `.*` wildcard in capturing groups. 
  For example, the pattern `building/(\\w+)/room/(\\d{1,4})/(.*)` is invalid because it will capture the whole subtopic of the pattern, and `$3` placeholder might include `/` characters, which will lead to an invalid Kafka topic name.
  * You can use the `.*` wildcard without a preceding `/` character, but it is not equivalent to when it is preceded by `/`. 
  For example, the pattern `building.*` will match `building`, `building/room`, `buildingroom`, and so on. 
  On the other hand, the pattern `building/.*` will match `building/room`, `building/floor/room`, and so on, but not `buildingroom` and `building/`. 
  * You can also use the `(?:\\/.*)?$` wildcard to match the whole subtopic level of the pattern. 
  For example, the pattern `locations/([^/]+)(?:\\/.*)?$` will match `locations/city/luanda/angola`, `locations/city`, `locations/city/`, and so on. 
  It's literally equivalent to `locations/+/#`.  
  Please note that the `(?:\\/.*)?$` wildcard is a non-capturing group, which means that it will not be used to replace the placeholders in the `kafkaTopic` and `kafkaKey` templates.
  * The `(?:\\/.*)?$` wildcard is different from the `.*` wildcard. For example, the pattern `sensors.*` will match everything after `sensors`, seperated with a slash or not. For example, `sensors`, `sensors/`, `sensorsdata`, and so on. 
  On the other hand, the pattern `sensors(?:\\/.*)?$` will match `sensors`, `sensors/`, `sensors/data`, and so on, but not `sensorsdata`.
* The expression `([^/]+)` is used to represent the wildcard `+`, which in turn represents a single level in the MQTT topic hierarchy.
It worth's mentioning that it is the user's responsibility to adhere to the [MQTT 3.1.1 naming conventions](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718106) when defining the MQTT topic patterns.

Placeholders in the `kafkaTopic` and `kafkaKey` templates are defined using the `$` character followed by a number. 
The number represents the index of the `capturing group` in the `mqttTopic` pattern. `Please note that the index starts from 1 and not 0`.
The MQTT Bridge uses the `capturing groups` in the `mqttTopic` pattern to positionally extract the values that will be used to replace the placeholders in the `kafkaTopic` and `kafkaKey` templates.

Let's go through each rule in the above example to understand how the MQTT Bridge uses these rules to map MQTT topics to Kafka topics:

1. MQTT Topic: `building/(\\w+)/room/(\\d{1,4}).*` -> Kafka Topic: `building_$1` with Kafka Key: `room_$2`

   This rule maps MQTT topics of the form `building/{some word}/room/{some number with 4 digits}/#` to the Kafka topic `building_$1`.
   For example, if the MQTT topic is `building/A/room/1003/floor/2` it will be mapped to the Kafka topic `building_A` with the key `room_1003`.

2. MQTT Topic: `sensors/([^/]+)/data` -> Kafka Topic: `sensor_data` with Kafka Key: `null`

   This rule maps MQTT topics of the form `sensors/+/data` to the Kafka topic `sensor_data`.
   For example, if the MQTT topic is `sensors/temperature/data`, it will be mapped to the Kafka topic `sensor_data`.
   Because the `kafkaKey` is not defined, the key of the Kafka record will be `null`.

3. MQTT Topic: `sensors.*` -> Kafka Topic: `sensor_others` with Kafka Key: `null`

   This rule maps any MQTT topic starting with `sensors/` followed by any number of levels in the hierarchy to the Kafka topic `sensor_others`.
   For example, if the MQTT topic is `sensors/temperature/living-room`, it will be mapped to the Kafka topic `sensor_others` with the key `sensor`.
   Because the `kafkaKey` is not defined, the key of the Kafka record will be `null`.

4. MQTT Topic: `devices/([^/]+)/data/(\b(all|new|old)\b)` -> Kafka Topic: `device_$1_data` with Kafka Key: `device_$2`

   This rule maps MQTT topics of the form `devices/{some word}/data/{either all, new or old}` to the Kafka topic `device_$1_data`.
   For example, if the MQTT topic is `devices/thermostat/data/all`, it will be mapped to the Kafka topic `device_thermostat_data` with the key `device_all`.
   This example also shows the advantage of using regex in the `mqttTopic` pattern. The last capturing group in the `mqttTopic` should be a word boundary `\b` followed by either `all`, `new` or `old` and anything else will not match the pattern.

The order in which the rules are defined is important.
The MQTT Bridge will use the first rule that matches the MQTT topic.
For example, if the MQTT topic is `sensors/temperature/data`, it will be mapped to the Kafka topic `sensor_data` because `sensors/([^/]+)/data` matches the MQTT topic before `sensors/#`.
If we swap the positions of the rules, the MQTT Bridge would use the `sensors.*` to map the MQTT topic to the Kafka  topic `sensor_others`.

### Bridge Configuration

The user can configure the MQTT Bridge using an `application.properties` file.
This section describes the configuration properties that can be used to configure the MQTT Bridge. 
The MQTT bridge can be configured using the appropriate prefix.
Following the prefixes for the specific configurations:

- `bridge.` is the prefix used for general configuration of the Bridge.
- `mqtt.` is the prefix used for MQTT configuration of the Bridge.
- `kafka.` is the prefix used for Kafka configurations of the Bridge.

A valid configuration file should look like this:

```properties
    # Bridge configuration
    bridge.id=my-bridge
    bridge.topic.default=default_topic
    # MQTT configuration
    mqtt.host=localhost
    mqtt.port=1883
    # Kafka configuration
    kafka.bootstrap.servers=localhost:9092
   ```

The following table describes the configuration properties defined above.

| Setting                 | Description                                                  | Default                 |
|-------------------------|--------------------------------------------------------------|-------------------------|
| bridge.id               | ID of the bridge                                             | null/undefined          |
| bridge.topic.default    | Topic to be used if no matches with any mapping rules        | messages_default        |
| mqtt.host               | Host address of the MQTT server                              | localhost               |
| mqtt.port               | Port number of the MQTT server                               | 1883                    |
| mqtt.max.bytes.message  | Max bytes in message for MQTT decoder                        | 8092                    |
| kafka.bootstrap.servers | Bootstrap servers for Apache Kafka                           | localhost:9092          |
| kafka.producer.*        | Any Kafka producer configuration (i.e. acks, linger.ms, ...) | Kafka producer defaults |


Other than the above properties, the user can also configure the bridge using environment variables.

## Contributing

You can contribute by:
- Raising any issues you find using Strimzi MQTT Bridge
- Fixing issues by opening Pull Requests
- Improving documentation
- Talking about Strimzi Kafka Bridge

All bugs, tasks or enhancements are tracked as [GitHub issues](https://github.com/strimzi/strimzi-mqtt-bridge/issues).
Issues which might be a good start for new contributors are marked with ["good-start"](https://github.com/strimzi/strimzi-mqtt-bridge/labels/good-start) label.

The [Building Strimzi MQTT Bridge](BUILDING.md) guide describes how to build Strimzi MQTT Bridge and how to test your changes before submitting a patch or opening a PR.

If you want to get in touch with us first before contributing, you can use:

- [Strimzi Dev mailing list](https://lists.cncf.io/g/cncf-strimzi-dev/topics)
- [#strimzi channel on CNCF Slack](https://slack.cncf.io/)

Learn more on how you can contribute on our [Join Us](https://strimzi.io/join-us/) page.

## License

Strimzi Kafka Bridge is licensed under the [Apache License](./LICENSE), Version 2.0