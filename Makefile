include ./Makefile.os
include ./Makefile.maven
include ./Makefile.docker

PROJECT_NAME ?= mqtt-bridge
GITHUB_VERSION ?= main
RELEASE_VERSION ?= latest

ifneq ($(RELEASE_VERSION),latest)
  GITHUB_VERSION = $(RELEASE_VERSION)
endif

.PHONY: all
all: java_verify docker_build docker_push

.PHONY: clean
clean: java_clean

.PHONY: release
release: release_prepare release_maven release_package

.PHONY: next_version
next_version:
	echo $(shell echo $(NEXT_VERSION) | tr a-z A-Z) > release.version
	mvn versions:set -DnewVersion=$(shell echo $(NEXT_VERSION) | tr a-z A-Z)
	mvn versions:commit

.PHONY: release_prepare
release_prepare:
	echo "Update release.version to $(RELEASE_VERSION)"
	echo $(shell echo $(RELEASE_VERSION) | tr a-z A-Z) > release.version
	echo "Update pom versions to $(RELEASE_VERSION)"
	mvn versions:set -DnewVersion=$(shell echo $(RELEASE_VERSION) | tr a-z A-Z)
	mvn versions:commit

.PHONY: release_maven
release_maven:
	echo "Update pom versions to $(RELEASE_VERSION)"
	mvn versions:set -DnewVersion=$(shell echo $(RELEASE_VERSION) | tr a-z A-Z)
	mvn versions:commit

.PHONY: release_package
release_package: java_package