# Venice
Logger/Replayer for sensor data and bridge between network protocols

Copyright (c) 2015 Dialogue Systems Group, University of Bielefeld.

* VeniceHub
    * logs sensor data to a file in UTF-8 format (can be packed with gzip), including sensor label, timestamp, data type and namespace
    * replays sensor data from a log file (raw or packed), can search position in log file with specific timestamp
    * bridge between network protocols (InstantReality, Robotics Service Bus, TCP)

* VeniceLib
    * primary library for VeniceHub
    * abstracts network protocols to independent interface
    * provides functions for parsing between the data and the strings representing the data

* Venice Kinect Server
    * connects to Kinect and sends data to TCP socket for VeniceHub
    * one server for Body data and one for Face data
    * it is intended for use only with the Kinect v2 Sensor

* Toy Server
    * two examples for servers to send data to network, so that VeniceHub can receive it
    * a java server to send data to InstantReality network
    * a python server to send data to Robotics Service Bus network

* ELAN mod
    * a modification for ELAN to send synchronization commands to VeniceHub
    * connects via Remote Procedure Call to VeniceHub
    * synchronizes replay of data by sending VeniceHub search commands

## Quick Usage Example

A very quick example, in which VeniceHub logs some input from the InstantIO toyserver to a log file.

As a preparation copy 'instantreality.jar' (e.g. from your InstantPlayer installation) to 'ToyServer/BasicServerIIOJava' and to 'VeniceHub/exported_jar/VeniceHub_lib'.

Open a console and change directory to ToyServer/BasicServerIIOJava. On Windows start the BasicServer with

    > java -cp .;* BasicServer

or on linux or mac start the BasicServer with

    > java -cp .:* BasicServer

Open another console and change directory to VeniceHub/exported_jar, then start VeniceHub with

    > java -jar VeniceHub.jar

In this example VeniceHub uses the default settings (input from InstantIO and output to file log.xio.gz).

Go back to the console of the BasicServer and type some lines of text.

Quit both programs by entering 'q' in each console.

Unzip the 'log.xio.gz' (or 'log_xxx.xio.gz' if this was not the first run) and open it with an editor and see the log.