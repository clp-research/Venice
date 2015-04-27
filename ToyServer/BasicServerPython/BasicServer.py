import sys
import time
import socket
import zlib

def main():
    DEFAULT_PORT = 54445
    
    if len(sys.argv) > 1:
      port = int(sys.argv[1])
    else:
      port = DEFAULT_PORT
      
    print "connecting to port " + str(port) + " ..."
    outputsocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    outputsocket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
    outputsocket.connect(("localhost", port))
    print "connection established"
    
    active = True
    while active:
        text = raw_input('> ')
        if text == 'q':
            active = False
        else:
            outputsocket.send(makeMessage(text))

def makeMessage(text):
    # add checksum and termination char
    cs = zlib.adler32(text) & 0xffffffff
    completedMessage = str(cs) + ", " + text + "\n"
    return completedMessage

if __name__ == '__main__':
    main()