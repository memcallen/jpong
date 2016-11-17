import serial
import sys

ser = serial.Serial()

ser.baudrate = 9600
ser.port = sys.argv[1]
ser.timeout = 0.3

print sys.argv[1]

if ser.port != 'null':
	ser.open()
	while ser.read(size=5) != "ready":
		print "Waiting"

while True:
	print ser.read(size=8)
	#print "050:100;" #uncomment for debugging
