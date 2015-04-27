ToyServer
=========
Simulates a server for kinect or leap sensor.
Connects as a TCP client to a TCP server (VeniceHub in VP-mode) and sends fake data.
This is to test the venice setup without sensor hardware.
Will stop when connection is closed by other side.

To start with the already compiled version, change directory to

    ToyServer/bin

Start the program with

    java toyserver.ToyServer [b|f|h] port
    
b - simulate body data
f - simulate face data
h - simulate hand data
port - TCP port number

VeniceHub has to be started first (see venice.bat as an example).