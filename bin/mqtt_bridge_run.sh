#!/bin/sh
set -x

# Find my path to use when calling scripts
MYPATH="$(dirname "$0")"

# Configure logging
if [ -z "$KAFKA_BRIDGE_LOG4J_OPTS" ]
then
      KAFKA_BRIDGE_LOG4J_OPTS="-Dlog4j2.configurationFile=file:${MYPATH}/../config/log4j2.properties"
fi

exec java $KAFKA_BRIDGE_LOG4J_OPTS -classpath "${MYPATH}/../libs/*" io.strimzi.kafka.bridge.mqtt.Main "$@"