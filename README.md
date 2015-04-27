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