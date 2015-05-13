#coding:utf-8
import sys
import smbus
import time
from Pubnub import Pubnub

PUBLISH_KEY = "<Your Publish Key>"
SUBSCRIBE_KEY = "<Your Subscribe Key>"
CHANNEL_C_CONTROL = "c-control";
SUBSCRIBE_CHANNELS= [CHANNEL_C_CONTROL]
SPEED_KEY = 'speed'
ADDRESS = 0x64
MIN_SPEED_BYTE = 0x08 # 0.64V
FAST_SPEED_BYTE = 0x2C # 3.53V

pubnub = Pubnub(PUBLISH_KEY, SUBSCRIBE_KEY)
i2c = smbus.SMBus(1)

# speed 0 - 100
def setSpeed(speed):
    if speed > 100:
        speed = 100
    elif speed < 0:
        speed = 0

    speedByte = MIN_SPEED_BYTE + (FAST_SPEED_BYTE - MIN_SPEED_BYTE) * speed / 100

    # Forward
    output = speedByte << 2;
    output += 0x01

    i2c.write_byte_data(ADDRESS, 0, output)

def subscribeCallback(message, channel):
    print('received, ' + channel + ':' + str(message))

    if channel == CHANNEL_C_CONTROL:
        speed = message['speed']
        setSpeed(speed)

def subscribeError(message):
    print(str(message))

pubnub.subscribe(SUBSCRIBE_CHANNELS, subscribeCallback, subscribeError)
