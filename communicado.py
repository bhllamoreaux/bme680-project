from pubnub.callbacks import SubscribeCallback
from pubnub.enums import PNStatusCategory
from pubnub.pnconfiguration import PNConfiguration
from pubnub.pubnub import PubNub
import psutil
import RPi.GPIO as GPIO
import time
import bme680
from helper_funcs import *

 
#set the publish and subscribe keys needed to authenticate the pubnub channel
pnconfig = PNConfiguration()
pnconfig.subscribe_key = "sub-c-"
pnconfig.publish_key = "pub-c-"
pnconfig.ssl = True
pubnub = PubNub(pnconfig)


#initialize the bme680 sensor
sensor = init_bme680()
pubnub.add_listener(MySubscribeCallback())
pubnub.subscribe().channels('communicado').execute()
#loop infinitely and send the data to my phone
while True:
	memory_percent = psutil.virtual_memory().percent
    	percent = psutil.cpu_percent(interval=1)
    	send_message("cpu_percent", percent, pubnub)
    	send_message("memory_percent", memory_percent,pubnub)
    
    	#if the sensor is ready, send data
    	if sensor.get_sensor_data():
		#celsius
		send_message("bme_temp",sensor.data.temperature,pubnub)
		#hectoPascals
		send_message("bme_pressure",sensor.data.pressure,pubnub)
		#% relative humidity
		send_message("bme_humidity",sensor.data.humidity,pubnub)
	
	#if the gas sensor is hot and ready to go, read the data
	if sensor.data.heat_stable:
		#Ohms
		send_message("bme_gas",sensor.data.gas_resistance,pubnub)

     	#sleep for 10 minutes before taking another reading
     	time.sleep(600)
	
    

