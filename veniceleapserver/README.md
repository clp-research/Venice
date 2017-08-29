## Venice Leap Server

Venice Leap Server is for connecting a Leap sensor with VeniceHub and log hand motion data using a Leap sensor. 
The x3d files are intended for visualizing the hand motion data with an [Intant Player](http://www.instantreality.org/downloads/).

## Usage
To log data from leap and visualize it, please follow these steps below:
 **Install the Leap SDK**:

  1.  Download Leap developer SDK from  to https://developer.leapmotion.com/
  2. extract the SDK and install leap
  3.  copy related files to leap folder
         
      1. *Mac*: Leap.py, libLeap.dylib, LeapPython.so
      2. *Ubuntu*: Leap.py, libLeap.so, LeapPython.so 
      3. *Windows*: Leap.dll, Leap.py, LeapPython.pyd
  

## Log hand motion data with the Leap server
run leap server in the terminal

**Fromat**



## Visualize data using Instant Player and Venice
  1. Open leap.x3d with Instantplayer, go to help-->Web Inteface Scenegraph --> Setup, set sysLogLevel to 'log' to turn off warnings. Because Splitters are used in the x3d file, if there are no hands in the tracking space, warnings will be printed in the console which slows down instantplayer. 
  2. Run VeniceHub: 
        go to exported_jar folder, start terminal and run following command: <br> `java -jar VeniceHub.jar -i VP --vpfile leap.xml --vpport 5444 -o IIO`
        it's also possible to replay logged data from the disk: <br>
        `java -jar VeniceHub.jar -i Disk -o IIO -f data.xio.gz` 
            

 3. Run Leap server:
            python leap_server.py localhost 5444
4. Quitting:
    Press 'Enter' to quit the leap_server.py, then VeniceIPC will quit automatically
