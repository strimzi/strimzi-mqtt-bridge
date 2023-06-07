import paho.mqtt.client as mqtt
import time
import signal
import os
from threading import Thread
from time import sleep
from multiprocessing import Process
import random


class MqttClient:
    def __init__(self, broker_address, broker_port):
        self.broker_address = broker_address
        self.broker_port = broker_port
        self.client = mqtt.Client()
        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message

    def connect(self):
        self.client.connect(self.broker_address, self.broker_port, 60)

    def disconnect(self):
        self.client.loop_stop()
        self.client.disconnect()

    def on_connect(self, client, userdata, flags, rc):
        print("Connected with result code " + str(rc))
        client.subscribe("my/topic")

    def on_message(self, client, userdata, msg):
        print("Received message on topic: " + msg.topic)
        print("Message: " + str(msg.payload.decode("utf-8")))

    def start(self):
        self.client.loop_start()

    def stop(self):
        self.client.loop_stop()

    def publish(self, topic, message):
        self.client.publish(topic, message)


messages = ["Doing", "Foo", "bar", "angola", "plant"]
topics = ["sensors/home", "devices/speakers", "/bluetooth", "/"]


def client_starter():
    broker_address = "localhost"
    broker_port = 1883
    mqtt_client = MqttClient(broker_address, broker_port)
    mqtt_client.connect()
    mqtt_client.start()
    msg = random.choice(messages)
    mqtt_client.publish(random.choice(topics), msg)
    print(f"Published {msg}")
    mqtt_client.stop()
    mqtt_client.disconnect()


def main():
    num_client = 20
    process = []
    try:
        for i in range(num_client):
            print(f'\n MQTTClient No {i + 1} \n')
            client = Process(target=client_starter)
            client.start()
            process.append(client)
            sleep(1)
    except Exception as e:
        if type(e) == KeyboardInterrupt:
            sleep(1)
            for p in process:
                os.kill(p.pid, signal.SIGINT)
                sleep(1)
        else:
            print(e)


if __name__ == '__main__':
    main()
