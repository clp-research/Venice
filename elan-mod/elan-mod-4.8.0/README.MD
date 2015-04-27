ELAN_MOD
========

Modification for ELAN to remote control external programs via RPC (especially VeniceHub).

Quick Install
-------------
Download and exctract ELAN source code version from

    https://tla.mpi.nl/tools/tla-tools/elan/download/

Be sure to download the version the mod is designed for (see name of elan-mod folder).  Copy the files from the elan-mod folder over the ELAN folder.  Two files will be overwritten:

    ElanFrame2.java
    pom.xml

There is one library that can not be installed by maven with online repositorys, so it has to be installed manually with:

    ./installLib.sh

Then build and start ELAN with:

    mvn package

Manual installation
-------------------
If an other version of ELAN needs to be modified, the quick installation may not work.  The patch can be applied by hand by an experienced user:

First copy all files from the elan-mod folder into the folder of your downloaded ELAN source code, but DO NOT overwrite

    ElanFrame2.java

Open both the original ElanFrame2.java and the modified ElanFrame2.java with an editor.  Find in the modified file the three parts marked with

    // ELAN_MOD Start
    ...
    // ELAN_MOD End
    
and copy those lines into the corresponding position in the ElanFrame2.java of your version (the first of them is an import).

The pom.xml has also to be changed. Again open the original version and the modified version and transfer the lines between

    <!-- ELAN_MOD Start -->
    ...
    <!-- ELAN_MOD End -->

Be sure to have the missing librarys installed, with:

   ./installLib.sh

Then recompile and start ELAN with:

    mvn package

Configuration
-------------
In the folder of ELAN should now be a file

    config.properties

It looks like

    #configuration for rpc browser connection (VeniceHub)
    browserIP=localhost
    browserPort=4243

    #configuration for remote controll connection (mumodo)
    remoteIP=localhost
    remotePort=4241

    #Frequency of synchronization in (ms)
    syncFreq=200

Those are default values.  The default browserPort is the same as the default rpcServerPort option for VeniceHub, so without any changes, ELAN should connect to VeniceHub (if VeniceHub is replaying from file and both running on the same computer).  ELAN will periodically send seek commands to VeniceHub to ensure synchronous replay.  This rate can be set with the syncFreq property.