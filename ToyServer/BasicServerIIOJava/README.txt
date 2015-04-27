BasicServer
===========

Demonstrates how to use VeniceLib to create a custom InstantIO server.

It creates a network-node and will send keyboard input to an out-slot.

It will also receive data from InstantIO (for example from another BasicServer).

Installation
------------
Copy the instantreality.jar (for example from your InstantPlayer installation) to the folder of BasicServer.

Usage
-----

    > java -cp .;* BasicServer [name]

The name command line argument will be used for the out-slot label.

Enter 'q' to quit the program.