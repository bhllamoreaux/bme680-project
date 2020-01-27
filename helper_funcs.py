from pubnub.callbacks import SubscribeCallback
from pubnub.enums import PNStatusCategory
from pubnub.pnconfiguration import PNConfiguration
from pubnub.pubnub import PubNub
import psutil
import RPi.GPIO as GPIO
import time
import bme680

def publish_callback(envelope, status):
    # Check whether request successfully completed or not
    if not status.is_error():
        pass # Message successfully published to specified channel.
    else:
        print("error sending message")
 
class MySubscribeCallback(SubscribeCallback):
    on = False

    def presence(self, pubnub, presence):
        pass  # handle incoming presence data
 
    def status(self, pubnub, status):
        if status.category == PNStatusCategory.PNUnexpectedDisconnectCategory:
            pass  # This event happens when radio / connectivity is lost
 
        elif status.category == PNStatusCategory.PNConnectedCategory:
            # Connect event. You can do stuff like publish, and know you'll get it.
            # Or just use the connected event to confirm you are subscribed for
            # UI / internal notifications, etc
            pass
        elif status.category == PNStatusCategory.PNReconnectedCategory:
            pass
            # Happens as part of our regular operation. This event happens when
            # radio / connectivity is lost, then regained.
        elif status.category == PNStatusCategory.PNDecryptionErrorCategory:
            pass
            # Handle message decryption error. Probably client configured to
            # encrypt messages and on live data feed it received plain text.
    
    def message(self, pubnub, message):
	#toggles the relay, to turn the lights on and off
        if message.message == 'button pressed':
            if self.on:
                self.on = False
            else:
                self.on = True

        if self.on:
            GPIO.output(11,GPIO.HIGH)
        else:
            GPIO.output(11,GPIO.LOW)

 
 #sends a message over the channel, chains the qualifier and payload together
 #qualifier is used to distinguish which information stream it is, so it can be parsed on 
 #the other side
def send_message(qualifier, payload, pubnub):
	#send the message!
    	pubnub.publish().channel('communicado').message(str(qualifier) + str(payload)).pn_async	(publish_callback)

#this function gets the distance from the ultrasonic distance sensor
def check_distance():
    try:
        GPIO.setmode(GPIO.BOARD)

        PIN_TRIGGER = 7
        PIN_ECHO = 11

        GPIO.setup(PIN_TRIGGER, GPIO.OUT)
        GPIO.setup(PIN_ECHO, GPIO.IN)

        GPIO.output(PIN_TRIGGER, GPIO.LOW)

        print("Waiting for sensor to settle")

        time.sleep(2)

        print ("Calculating distance")

        GPIO.output(PIN_TRIGGER, GPIO.HIGH)

        time.sleep(0.00001)

        GPIO.output(PIN_TRIGGER, GPIO.LOW)

        while GPIO.input(PIN_ECHO)==0:
            pulse_start_time = time.time()
        while GPIO.input(PIN_ECHO)==1:
            pulse_end_time = time.time()

        pulse_duration = pulse_end_time - pulse_start_time
        distance = round(pulse_duration * 17150, 2)
        print ("Distance:",distance,"cm")
        return distance

    finally:
        GPIO.cleanup()

#initializes the bme680 sensor and prepares it to be read
def init_bme680():
	#create the sensor object
	try:
		sensor = bme680.BME680(bme680.I2C_ADDR_PRIMARY)
	except IOError:
    		sensor = bme680.BME680(bme680.I2C_ADDR_SECONDARY)

	#most of this code is copied from this wonderful tutorial 
	#https://learn.pimoroni.com/tutorial/sandyj/getting-started-with-bme680-breakout
	#first we set the oversample, as well as set the filter to smooth out the data
	#the oversample can be modified to change the balance between noise and accuracy
	sensor.set_humidity_oversample(bme680.OS_2X)
	sensor.set_pressure_oversample(bme680.OS_4X)
	sensor.set_temperature_oversample(bme680.OS_8X)
	sensor.set_filter(bme680.FILTER_SIZE_3)

	#this just enables the gas sensor
	#since it needs a hot plate, we also have to set the heat duration and temperature
	#probably shouldn't have to be messed with in the majority of cases
	sensor.set_gas_status(bme680.ENABLE_GAS_MEAS)
	sensor.set_gas_heater_temperature(320)
	sensor.set_gas_heater_duration(150)
	sensor.select_gas_heater_profile(0)

	return sensor

#initializes the relay and sets it to low to begin
def init_relay():
    GPIO.setwarnings(False)
    GPIO.setmode(GPIO.BOARD)
    GPIO.setup(11,GPIO.OUT)
    GPIO.output(11,GPIO.LOW)
