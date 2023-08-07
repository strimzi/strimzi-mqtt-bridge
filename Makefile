include ./Makefile.os
include ./Makefile.maven
include ./Makefile.docker

PROJECT_NAME ?= mqtt-bridge

.PHONY: all
all: java_verify docker_build docker_push

.PHONY: clean
clean: java_clean