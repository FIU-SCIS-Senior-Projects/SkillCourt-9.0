import time
import socket
import threading				
import subprocess				#accessing shutdown
from six.moves import input 			#temporary, to control flow
from zeroconf import ServiceBrowser, Zeroconf	#for service discovery nds
import Adafruit_GPIO.SPI as SPI                 #SPI imports for connection to analog sensor
import Adafruit_MCP3008                         #analog to digital conversion
from neopixel import *                          #modules for ws2812b LED strip
import uuid

server_address = ""
server_port  = 0
UUID =  str(hex(uuid.getnode()))

class Client(object):
	def __init__(self, server, port):
		self.server = server
		self.port = port
		self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		self.sock.settimeout(100)
		try:
			self.sock.connect((self.server, self.port)) 			#connects to app service
			listen = threading.Thread(target = self.acceptCommand)		#begins a dedicated listening thread to receive commands
			listen.start()
			print "connection established"
		except:
			print "Could not connect to server at address: ", self.server, "port: ", self.port #when service, address, and port are published, but connection does not work



	def acceptCommand(self):
		#uncomment for debug
		print "acceptCommand thread started"
		buffer = 100
		cl = CommandList()
		while True:
			try:
				command = self.sock.recv(buffer)		#reads buffer when available
				if command:
					#uncomment for debug
					#print "Received:", command
					#response = "got it: " + command
					#self.sock.send(response)

					execute  = threading.Thread(target = cl.call, args = (command, self.sock, ))	#begins new thread to execute each new command
													#passes socket to thread so commands can communicate
					execute.start()
					print "started command execution"	
				else:
					print "disconnected from server"
					self.sock.close()			#if connection lapses, closes socket
					break
			except:
				#for debug
				print "when does this work?"
				print time.time()
				pass

	
	def sendCommand(self, info):		#not used
		try:
			self.sock.send(info)
		except:
			print "server unreachable"


#LED STRIP OBJECT
class LEDS(object):
	LED_FREQ_HZ	= 800000	#LED signal frequency in hertz (usually 800khz)
        LED_DMA		= 5		#DMA channel to use for generating signal (try 5)
        LED_BRIGHTNESS	= 255		#Set to 0 for darkest and 255 for brightest may have to adjust for power consumption efficiency
        LED_INVERT	= False		#True to invert the signal (when using NPN transistor level shift)
        LED_CHANNEL	= 0		#set to '1' for GPIOs 13, 19, 41, 45 or 53
        LED_STRIP	= ws.WS2811_STRIP_GRB	#Strip type and colour ordering
	COLUMN_LENGTH	= 5		#set for skill court pad v1.1
	ROW_LENGTH	= 41		#set for skill court pad v1.1
	LED_PIN		= 18		#set for skill court pad v1.1 (has PWM)

	lights_off	= threading.Event() #creates object controlled thread event that can be remote triggered (not used)

	#LED numerical setup
	# ROW(1) {increasing>>>>>>>>>>>>>>>>>}
	# ROW(2) {<<<<<<<<<<<<<<<<<increasing}
	# ROW(3) {increasing>>>>>>>>>>>>>>>>>} . . . etc
	

	def __init__(self):
		self.LED_COUNT = self.COLUMN_LENGTH * self.ROW_LENGTH
		self.strip = Adafruit_NeoPixel(self.LED_COUNT, self.LED_PIN, self.LED_FREQ_HZ, self.LED_DMA, self.LED_INVERT, self.LED_BRIGHTNESS, self.LED_CHANNEL, self.LED_STRIP)
		self.strip.begin()
		self.TARGET_COLOR = 0x00ffff	#TURQUOISE
		self.MISTAKE_COLOR = 0xff0000	#RED
		self.POINT_COLOR = 0x00ff00	#GREEN

	def clear_all(self):			#Sets all pixels to black
		print "clearing all"
		for i in range(0, self.strip.numPixels()):
			self.strip.setPixelColor(i, 0)
		self.strip.show()

	def flash_rows(self, color, delay):	#Flashes Rows Solid from top to bottom
		print "flashing rows"
		for i in range(0, self.COLUMN_LENGTH):
			for j in range(i*self.ROW_LENGTH, i*self.ROW_LENGTH + self.ROW_LENGTH):
				self.strip.setPixelColor(j, color)
			self.strip.show()
			time.sleep(delay)
			self.clear_all()

	def flash_columns(self, color, delay):	#Flashes Columns Solid from left to right
		print "flashing rows"
		for i in range(0, self.ROW_LENGTH):
			for j in range(i*self.COLUMN_LENGTH, i*self.COLUMN_LENGTH + self.COLUMN_LENGTH):
				self.strip.setPixelColor(j, color)
			self.strip.show()
			time.sleep(delay)
			self.clear_all()
	def linear(self, gap, color, delay):	#Lights up LEDS in numerical order
		print "linear"
		i = 0
		while i < self.strip.numPixels():
			self.strip.setPixelColor(i, color)
			self.strip.show()
			i = i + 1 + gap
			time.sleep(delay)

	def flash_all(self, color, gap, times, delay):	#flashes all LEDs at once based on interval
		print "flash all"
		for i in range(0, times):
			j = 0
			while j < self.strip.numPixels():
				self.strip.setPixelColor(j, color)
				j = j + 1 + gap
			self.strip.show()
			time.sleep(delay)
			self.clear_all()

	def all_on(self, color, gap):			#lights all LEDs solid based on gap, until clear_all() is called
		print "all on"
		i = 0
		while i < self.strip.numPixels():
			self.strip.setPixelColor(i, color)
			i = i + 1 + gap
		self.strip.show()


#Creates object to monitor vibration sensor
class ImpactSensor(object):
        SENSOR_CHANNEL = 7 #analog input channel (set for skill court pad v1.1)
        THRESHOLD = 500		#sensitivity of impact sensor 1-1024, 1024 being least sensitive
        sensor_on = threading.Event()
        impact_detected = threading.Event()

        def __init__(self):		#analog read is controlled by SPI hardware interface
                self.SPI_PORT = 0
                self.SPI_DEVICE = 0
                self.meas = Adafruit_MCP3008.MCP3008(spi=SPI.SpiDev(0, 0))

        def read(self):
                print "Read from channel 7"
                print self.meas.read_adc(self.SENSOR_CHANNEL)

        def waitForImpact(self):
                print "Beginning 'Detect Impact' loop"
                while self.sensor_on.isSet():
			time.sleep(.1)					#sleeps for 1/10 second to reduce CPU usage
                        while self.impact_detected.isSet():		#wait for reset of event handler
                                pass
                        while self.meas.read_adc(self.SENSOR_CHANNEL) < self.THRESHOLD and self.sensor_on.isSet():
                                pass
			if self.sensor_on.isSet(): #to ensure that impact is not registered after a game has ended
                        	print "Impact detected"
                        	self.impact_detected.set()
				time.sleep(1) #waits 1 second for values to return to normal
			else:
				break
		print "Sensor Off"



#Command list Classs handles all functions of board and game
class CommandList:
	#Commands to send to App
	CONNECTED = "1\n"
	DISCONNECTED = "2\n"
	IMPACT_DETECTED = "3\n"

	leds = LEDS()							#creates dedicated LED object
	sensor = ImpactSensor()						#creates dedicated senor object


	target_on = threading.Event()
	accept_target_color = threading.Event()

	def call(self,NUM, socket):
		#print "call function reached"
		if(NUM == "0"):
			print "Received 0 - Connection Terminated"
			socket.close()

		elif(NUM == "1"):
			print "Received 1 - Connection Established"
			socket.send(self.CONNECTED)
			socket.send(UUID+"\n")

			self.leds.flash_rows(0x000080, 1)		#0x000080 = NAVY BLUE

		elif(NUM == "2"):
			print "Received 2 - Sensor On, Detecting Impacts"
			self.sensor.sensor_on.set()
			sensor = threading.Thread(target = self.sensor.waitForImpact)
			sensor.start()

			while self.sensor.sensor_on.isSet():
				self.sensor.impact_detected.wait()
				socket.send(self.IMPACT_DETECTED)
				if self.target_on.isSet():
					self.leds.flash_all(self.leds.POINT_COLOR, 1, 1, .5)
					self.target_on.clear()
				else:
					self.leds.flash_all(self.leds.MISTAKE_COLOR, 1, 1, .5)
				self.sensor.impact_detected.clear()
			print "sensor turned off, closing thread"

		elif(NUM == "3"):
			print "Target On"
			self.target_on.set()
			self.leds.all_on(self.leds.TARGET_COLOR, 1)

		elif(NUM == "4"):
			print "Received " + NUM + "Turning off sensor"
			self.sensor.sensor_on.clear()


		elif(NUM == "5"):
			print "waiting for target color assignment"
			self.accept_target_color.set()
			print self.leds.TARGET_COLOR

		elif(NUM == "6"):
			print "6"

		elif(NUM == "99"):
			print NUM
			u = Utilities()
			u.shutdown()
		elif self.accept_target_color.isSet():
			print hex(NUM)
			self.leds.TARGET_COLOR = hex(NUM)
			self.accept_target_color.clear()
		else:
			print "not recognized", NUM
			#return "unkown"


#app service discovery
#thank you https://pypi.python.org/pypi/zeroconf
class MyListener(object):

        def remove_service(self, zeroconf, type, name):
                print("Service %s removed" % (name,))

        def add_service(self, zeroconf, type, name):
                global server_address, server_port
                info = zeroconf.get_service_info(type, name)
                print("Service %s added, service info: %s" % (name, info))
                if info.name.find('app')  >= 0:         #if skillcourtapp is in name, save info to glob vars
                        server_address = socket.inet_ntoa(info.address)
                        server_port = info.port
                        print "ServerAddress and Port Found: ",server_address, server_port


#used for background services 
class  Utilities(object):

	def findAppConnection(self, event_handler):
		print "looking for server address and port of Skill Court App"
		thread = threading.Thread(target = self.appBrowse, args = (event_handler,))
		thread.start()
		print "App browsing thread started"

	def appBrowse(self, event_handler):
                zeroconf = Zeroconf()
                listener = MyListener()
                browser = ServiceBrowser(zeroconf, "_http._tcp.local.", listener)
                while server_port == 0:
                        pass            
		event_handler.set()
                zeroconf.close()
                print "closed zeroconf"
		return True
	def shutdown(self):
                print "Shutting Down"
                command = "/usr/bin/sudo /sbin/shutdown now"
                process = subprocess.Popen(command.split(), stdout=subprocess.PIPE)
                output = process.communicate()[0]

	def reboot(self):
                print "Rebooting"
                command = "/usr/bin/sudo /sbin/shutdown -r now"
                process = subprocess.Popen(command.split(), stdout=subprocess.PIPE)
                output = process.communicate()[0]



#main thread
if __name__ == "__main__":
	server_found = threading.Event()	#creates threading event handler for service detection
	Utilities().findAppConnection(server_found)
	print "waiting for discovery"
	server_found.wait()			#waits for service to be found
	connection = Client(server_address, server_port)
	print "all main thread tasks underway"
