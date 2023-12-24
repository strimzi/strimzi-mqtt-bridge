include ./Makefile.os
include ./Makefile.maven
include ./Makefile.docker

PROJECT_NAME ?= mqtt-bridge
RELEASE_VERSION ?= latest

.PHONY: all
all: java_package docker_build docker_push

.PHONY: clean
clean: java_clean

.PHONY: release
release: release_prepare release_maven release_version release_package

.PHONY: next_version
next_version:
	echo $(shell echo $(NEXT_VERSION) | tr a-z A-Z) > release.version
	mvn versions:set -DnewVersion=$(shell echo $(NEXT_VERSION) | tr a-z A-Z)
	mvn versions:commit

.PHONY: release_prepare
release_prepare:
	echo "Update release.version to $(RELEASE_VERSION)"
	echo $(shell echo $(RELEASE_VERSION) | tr a-z A-Z) > release.version
	rm -rf ./strimzi-mqtt-bridge-$(RELEASE_VERSION)
	rm -f ./strimzi-mqtt-bridge-$(RELEASE_VERSION).tar.gz
	rm -f ./strimzi-mqtt-bridge-$(RELEASE_VERSION).zip
	mkdir ./strimzi-mqtt-bridge-$(RELEASE_VERSION)

.PHONY: release_version
release_version:
	echo "Changing Docker image tags in install to :$(RELEASE_VERSION)"
	$(FIND) ./packaging/install -name '*.yaml' -type f -exec $(SED) -i '/image: "\?quay.io\/strimzi\/[a-zA-Z0-9_.-]\+:[a-zA-Z0-9_.-]\+"\?/s/:[a-zA-Z0-9_.-]\+/:$(RELEASE_VERSION)/g' {} \;

.PHONY: release_maven
release_maven:
	echo "Update pom versions to $(RELEASE_VERSION)"
	mvn versions:set -DnewVersion=$(shell echo $(RELEASE_VERSION) | tr a-z A-Z)
	mvn versions:commit

.PHONY: release_package
release_package:
	$(CP) -r ./packaging/install ./
	$(CP) -r ./packaging/install ././strimzi-mqtt-bridge-$(RELEASE_VERSION)/
	tar -z -cf ./strimzi-mqtt-bridge-$(RELEASE_VERSION).tar.gz strimzi-mqtt-bridge-$(RELEASE_VERSION)/
	zip -r ./strimzi-mqtt-bridge-$(RELEASE_VERSION).zip strimzi-mqtt-bridge-$(RELEASE_VERSION)/
	rm -rf ./strimzi-mqtt-bridge-$(RELEASE_VERSION)
	$(FIND) ./packaging/install/ -mindepth 1 -maxdepth 1 ! -name Makefile -type f,d -exec $(CP) -rv {} ./install/ \;