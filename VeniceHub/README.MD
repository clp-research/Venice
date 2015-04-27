VeniceHub
=========

This is only a quick guide, for more information see manual/VeniceHubManual.pdf

Usage examples (starting the program):
--------------

Logging from InstantIO to log.xio.gz (filename will be changed if a file with this name already exists):

    $ java -jar VeniceHub.jar

Logging from InstantIO to myfile.xio without compression:

    $ java -jar VeniceHub.jar -f myfile.xio --writeRaw

Replaying from myfile.xio.gz to InstantIO:

    $ java -jar VeniceHub.jar -i Disk -f myfile.xio.gz -o IIO

Replaying with an offset of 10235 ms:

    $ java -jar VeniceHub.jar -i Disk -f myfile.xio.gz -o IIO --offset 10235
    
Translating from IIO to RSB:

    $ java -jar VeniceHub.jar -i IIO -o RSB --classMatcher ..\match.xml --protobuf ..\..\venicestuff\protobuf

Getting help:

    $ java -jar VeniceHub.jar -h

Quitting:
---------

Type 'q' and press Enter.  Don't be confused if your typing is not displayed in the console.

Checking the configuration in runtime:
--------------------------------------

Type 'config' and press Enter.

Run VeniceHub with InstantIO multicast on Linux:
------------------------------------------------

    $ java -Djava.net.preferIPv4Stack=true -jar VeniceHub.jar ...

Turning on/off debugging messages
---------------------------------
Edit the log4j.properties and replace DEBUG with INFO.