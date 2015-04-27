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

Format
------
The Venice Kinect Servers send to a TCP socket a String representation of the data.  The ﬁelds have to be separated by a comma and a space.  The ﬁrst value has to be the checksum (Adler32).  The string has to end with a newline character.

Multiﬁelds: A multiﬁeld has to start with an integer, which indicates the number of ﬁelds belonging to the multiﬁeld.  The subﬁelds in a multiﬁeld have also to be separated with a comma and a space.

Example: A MFVec2f (2D floating point vector) with three ﬁelds is formated like

    3, 0.11, 0.12, 0.21, 0.22, 0.31, 0.32

It is possible to have arrays with zero ﬁelds.

Kinect Server for _body_ data: A data string from the kinect body server consists of six MFVec3f (3D floating point vector), each containing 25 vectors with three values and representing a skeleton.  So if the maximum number of six skeletons are tracked by the Kinect, the string would contain 451 (1+6*3*25) comma separated values (including the checksum).  The 25 vectors are describing the joints of a skeleton, see the Kinect v2 SDK documentation for the definition of the joints ('JointType'), see https://msdn.microsoft.com/en-us/library/microsoft.kinect.jointtype.aspx

Kinect Server for _face_ data: A data string from the kinect face server consists of seven multifield 2D vectors, a multifield rotation vector (four values), and a multifield string of eight characters.  For each tracked face there is one field in all of the multifields.

No | Content              | Values
--:| -------------------- | -----------
 0 | FaceBoxTopLeft       | x, y
 1 | FaceBoxBottomRight   | x, y
 2 | FaceEyeLeft          | x, y
 3 | FaceEyeRight         | x, y
 4 | FaceNose             | x, y
 5 | FaceMouthLeftCorner  | x, y
 6 | FaceMouthRightCorner | x, y
 7 | FaceRotation         | w, x, y, z
 8 | FaceProperties       | HEGLROMA

The FaceProperties are an eight-characters-string ('HEGLROMA') with the following meaning:

Char | Meaning
----:| ----------------
   H | Happy
   E | Enganged
   G | Glasses
   L | Left eye closed
   R | Right eye closed
   O | Mouth open
   M | Mouth moved
   A | Looking away

Each character can be one of four numbers: 0 (unknown), 1 (no), 2 (maybe) or 3 (yes).

If the maximum of six faces are tracked, the data string would contain 115 comma separated values (1+6*19), including the checksum.

See also https://msdn.microsoft.com/en-us/library/microsoft.kinect.face.faceproperty.aspx


Privacy
-------

_Note_: When using the Venice Kinect Servers with a Kinect for Windows v2 sensor, Microsoft will collect telemetry data (e.g., operating system, number of processors, graphic chipset, memory, device type, locale, time) in order to improve Microsoft products and services. The data will not be used to identify specific individuals.
