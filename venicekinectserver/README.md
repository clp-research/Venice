Venice Kinect Servers
=====================

Venice Kinect Servers BodyBasics-D2D and FaceBasics-D2D are for connecting a Kinect v2 Sensor with VeniceHub.

Venice Kinect Servers are intended for use only with the Kinect v2 Sensor.

Files for the modified KinectServer
-----------------------------------

Release : Contains .exe file that starts the Kinect Server

BodyBasics-D2D : Kinect Server for Body data

FaceBasics-D2D : Kinect Server for Face data

vpfiles : XML files with sensor slot definitions for VeniceHub VP-mode

x3dfiles : X3D files for InstantPlayer to visualize kinect data

ToyServer : to simulate sensor data


Important
---------

_Important_ for building the exe: Add ws2_32.lib to the linker! Otherweise the creation of sockets will fail while building the project.

_Important_:

In BodyBasics-D2D the file 'BodyBasics-D2D.sdf' is not included (50 MB).

In FaceBasics-D2D the file 'FaceBasics-D2D.sdf' is not included (52 MB).

Usage
-----

In the subfolder Release are .exe files to start the kinect server.  
They take one command line argument for the port number to connect to VeniceHub in VP-mode.  
The default value is 5005.

Privacy
-------

_Note_: When using the Venice Kinect Servers with a Kinect for Windows v2 sensor, Microsoft will collect telemetry data (e.g., operating system, number of processors, graphic chipset, memory, device type, locale, time) in order to improve Microsoft products and services. The data will not be used to identify specific individuals.
