# CHANGELOG

## 0.2.0

* Moved to UBI9 micro as the base container image
* Use Java 21 as the runtime in the MQTT Bridge container image
* Use the correct `mqtt.host` and `mqtt.port` configuration options in sample configuration files and docs instead of incorrect `mqtt.server.host` and `mqtt.server.port`
* Support for TLS/SSL Encryption
* Use Java 25 as the runtime in the container (leaving the language level at Java 21) for supporting Post-Quantum Hybrid Key Exchange

## 0.1.0

* First release of the MQTT bridge
