FROM registry.access.redhat.com/ubi8/ubi-minimal:latest
ARG JAVA_VERSION=17
ARG TARGETPLATFORM

USER root

RUN microdnf update \
    && microdnf --setopt=install_weak_deps=0 --setopt=tsflags=nodocs install java-${JAVA_VERSION}-openjdk-headless openssl shadow-utils \
    && microdnf clean all

# Set JAVA_HOME env var
ENV JAVA_HOME /usr/lib/jvm/jre-${JAVA_VERSION}

# Add strimzi user with UID 1001
# The user is in the group 0 to have access to the mounted volumes and storage
RUN useradd -r -m -u 1001 -g 0 strimzi

ARG strimzi_mqtt_bridge_version=1.0-SNAPSHOT
ENV STRIMZI_MQTT_BRIDGE_VERSION ${strimzi_mqtt_bridge_version}
ENV STRIMZI_HOME=/opt/strimzi
RUN mkdir -p ${STRIMZI_HOME}
WORKDIR ${STRIMZI_HOME}

COPY target/mqtt-bridge-${strimzi_mqtt_bridge_version}/mqtt-bridge-${strimzi_mqtt_bridge_version} ./

USER 1001

CMD ["/opt/strimzi/bin/mqtt_bridge_run.sh"]