#!/usr/bin/env bash
set +x

# Clean-up /tmp directory from files which might have remained from previous container restart
# We ignore any errors which might be caused by files injected by different agents which we do not have the rights to delete
rm -rfv /tmp/* || true

MYPATH="$(dirname "$0")"

# Configure Memory
. "${MYPATH}"/dynamic_resources.sh

MAX_HEAP=$(get_heap_size)
if [ -n "$MAX_HEAP" ]; then
  echo "Configuring Java heap: -Xms${MAX_HEAP}m -Xmx${MAX_HEAP}m"
  export JAVA_OPTS="-Xms${MAX_HEAP}m -Xmx${MAX_HEAP}m $JAVA_OPTS"
fi

# Disable FIPS if needed
if [ "$FIPS_MODE" = "disabled" ]; then
    export JAVA_OPTS="${JAVA_OPTS} -Dcom.redhat.fips=false"
fi

# starting MQTT Bridge with final configuration
exec /usr/bin/tini -s -w -e 143 -- "${MYPATH}"/../mqtt_bridge_run.sh --config-file="${STRIMZI_HOME}"/config/application.properties --mapping-rules="${STRIMZI_HOME}"/config/topic-mapping-rules.json "$@"
