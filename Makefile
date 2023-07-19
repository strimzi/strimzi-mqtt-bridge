include ./Makefile.os
include ./Makefile.maven

PROJECT_NAME ?= mqtt-bridge

.PHONY: all
all: java_verify

.PHONY: clean
clean: java_clean
