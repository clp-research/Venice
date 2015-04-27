BasicServer
===========

Demonstrates how to use pythen to create a TCP socket connection to VeniceHub.

It will send keyboard input to VeniceHub.

Before starting it, VeniceHub needs to be started in VP mode with the right port number and the vpfile.xml included in this package. Example:

    > java -jar VeniceHub.jar -i VP --vpport 54445 --vpfile vpfile.xml

Usage
-----

    > python BasicServer.py [port]

The port command line argument will be used for the TCP socket port.

Enter 'q' to quit the program.