= venice.lib (26. Nov. 2014)

Provides methods for connection with instantIO (instantreality) and RSB network and parsing of XIO lines.


== InstantIO (IIO)

See the API for detailed information on each class and method.  Here is only the order given how to create and use InstantIO via venice.lib.

The order creating InstantIO network node and slots:

Most important class is _IIONamespaceBuilder_.  It is static and no instances can be created.  It hides away every class and object that belongs to InstantIO.  An application should use only the abstract ways of accessing the InstantIO, provided by IIONamespaceBuilder.

First call _setXioCodesFilename_ and give the name of the file with your XIO tag codes.

Then use _setSlotFlags_ to define the setting for importing and exporting of slots for the network node.  By default they are set to false.  If you use InstantIO for logging, then importing should be enabled.  If you use InstantIO for sending data, you can activate exporting (but try first without).  At this step it is also important to think about the TTL (time to live) for data sended via InstantIO network.  By default the TTL is set to 0 (will not leave localhost).  If you want to change it, use _setMulticastTTL_.

Use _prepareNamespace_ to create a namespace with the given label.  This will create an InstantIO namespace object (hidden away).

_initializeOutSlots_ and _initializeInSlots_ are used to initialize slots, either with a given list of abstract slot definitions, or with null.  The latter causes dynamic slot creation.  If using abstract slot definitions, the slots will be created immediately.  They are hidden by venice.lib and can not be accessed directly by the application.

_setMasterInSlotListener_ is important for the use of in-slots.  The application has to provide an object that implements AbstractSlotListener, the so called *master listener*.  From now on, if an in-slot receives data, venice.lib will pass this data to the master listener.

_write_ is important for the use of out-slots.  If the application wants to send data to an out-slot, it calls _write_.  If in predefined mode data will be ignored if the demanded out-slot doesn't exist.  If in dynamic mode, the out-slot will be created.

_setMulticastAddress_ and _setMulticastPort_ are for setting up the multicast connection.

It is possible to set a prefix for the namespaces created on this node with setNodePrefix.  By default it is set to "{SlotLabel}".

To parse a XML file with sensor definitions, use the static parse method from the SensorFileReader class.


=== Examples

Example 1 - create a listening namespace that receives everything

    public class MyIIOReader implements AbstractSlotListener{
    
      public MyIIOReader(){
        IIONamespaceBuilder.setXioCodesFilename("myXioCodes.XML");
        IIONamespaceBuilder.setSlotFlags(new SlotFlags(true, false));
        IIONamespaceBuilder.initializeInSlots(null);
        IIONamespaceBuilder.setMasterInSlotListener(this);
      }
			
      public void newData(Object data, Class<?> type, String label) {
        // do something with the data
      }
      
    }

		
Example 2 - prepare writing to pre-defined out-slots with sensor file

    public class MyIIOWriter{
    
      public MyIIOWriter(){
        IIONamespaceBuilder.setXioCodesFilename("myXioCodes.XML");
        IIONamespaceBuilder.setSlotFlags(new SlotFlags(false, true));
        AbstractSlot[] slotArray = SensorFileReader.parse(new File("sensor.xml"));
        IIONamespaceBuilder.initializeOutSlots(slotArray);
      }
      
      private void writeSomething(String slotLabel, Object something, String namespace){
        IIONamespaceBuilder.write(slotLabel, something, namespace);
      }
      
    }


== RSB

Configuration of RSB

To configure RSB, create a rsb.conf file in one of the following ways:

    1) /etc/rsb.conf
    2) $HOME/.config/rsb.conf
    3) $(pwd)/rsb.conf
    4) %userprofile%\.config\rsb.conf

Possibility 4 is for Windows.  For every operating system the creation of the rsb.conf file in the working directory of the application works, too.

The rsb.conf looks usually like this:

    [transport.spread]
    host    = localhost
    port    = 4803
    enabled = 0
    [transport.socket]
    enabled = 1
    host = localhost


See the API for detailed information on each class and method.  Here is only the order given how to create and use RSB via venice.lib.

The order creating RSB namespaces(= scopes), in-slots (= listeners) and out-slots (= informers):

Use _setProtobufDir_ to give the directory where the protobuf classes are stored.

After that, it is possible to call _setMatchFile_ and name a XML file with class match definitions, if this is needed.

Call then _setXioCodesFilename_ and give the name of the file with your XIO tag codes.

With _setPrefix_ it is possible to set the scope for dynamic mode.  Without prefix, dynamic slot creation will happen on the scope '/'.  Once a prefix is set, it will be added to all scopes.

Call _initializeProtobuf_ to load all protobuf classes and to register them for the use with RSB.  From now on, the protobuf classes will be known, that means, the application can read sensor XML files, where such classes are named, without crashing caused by unknown classes.

_initializeOutSlots_ and _initializeInSlots_ are used to initialize slots, either with a given list of abstract slot definitions, or with null.  The latter causes dynamic slot creation.  If using abstract slot definitions, the slots will be created immediately.  They are hidden by venice.lib and can not be accessed directly by the application.

_setMasterInSlotListener_ is important for the use of in-slots (= listeners).  The application has to provide an object that implements AbstractSlotListener, the so called *master listener*.  From now on, if an in-slot receives data, venice.lib will pass this data to the master listener.

_write_ is important for the use of out-slots (= informers).  If the application wants to send data to an out-slot, it calls _write_.  If in predefined mode data will be ignored if the demanded out-slot doesn't exist.  If in dynamic mode, the out-slot will be created.


=== How to create protobuf classes

See the proto files that are included.  They are located in venice.lib\protos and where used to create the protobuf classes used by VeniceHub.  They should have the following structure:

    package protobuf;
    option java_package = "protobuf";
    option java_outer_classname = "XXXProtos";
    message XXX {
      // your values
    }

Where XXX is the name of your class.  The name of the file has to be XXX.proto.

Example for single field boolean:

    package protobuf;
    option java_package = "protobuf";
    option java_outer_classname = "BoolProtos";
    message Bool {
      optional bool value = 1;
    }

Note: The assigned value is not an actual value, but an index (starting with 1).  See google protocol buffers documentation for more information.

Example for multifield float:

    package protobuf;
    option java_package = "protobuf";
    option java_outer_classname = "MFFloatProtos";
    message MFFloat {
      repeated float value = 1 [packed=true];
    }

Create a folder named protobuf and copy your proto files one level above.

Example:

    create folder /myApplication/defs/protobuf
    copy your proto files to /myApplication/defs/

Compile each proto file with protoc (get it from google and copy it in the folder where the proto files are located).  The protobuf jar (e.g. protobuf-java-2.4.1.jar) has to be in that folder, too.

    > protoc --java_out=. XXX.proto

This creates a XXXProtos.java in the protobuf folder.  This source file can already be used in your source code.  But for the use with venice.lib, further compiling is necessary.

Change directory to the protobuf folder, which should now contain the above mentioned java files created by protoc.  Type the following command:

    > javac -cp .;..\protobuf-java-2.4.1.jar XXXProtos.java

This creates all class files.  venice.lib should recognize them now.


=== How to tell venice.lib what protobuf class belongs to what XIO code

For what is this important?   If an object is written into a log file, the assigned XIO code will be used.  For example, by default a String object is coded as sfstring.  And the XIO parser uses this for creating objects from a log file.

The assignment of classes with XIO codes is done with a XML file.

Example:

    <?xml version="1.0"?>
    <codes clear="true">
      <def class="protobuf.Int32Protos$Int32" code="sfint32"/>
      <def class="protobuf.FloatProtos$Float" code="sffloat"/>
      <def class="protobuf.MFFloatProtos$MFFloat" code="mffloat"/>
      <def class="protobuf.Vec3fProtos$Vec3f" code="sfvec3f"/>
      <def class="protobuf.MFVec3fProtos$MFVec3f" code="mfvec3f"/>
    </codes>

The class named in the class attribute is then assigned to the XIO code named in the code attribute.  The clear="true" attribute in the prime tag tells venice.lib to clear all default definitions (there are default XIO code definitions for some java native types, like String, Float, ...).  Otherwise, the new ones from the file are added.


=== How to link a protobuf class to an InstantIO class for RSB-to-IIO / IIO-to-RSB modes?

What is matching good for? An protobuf class object, received by RSB, needs to be converted into an IIO class object. For example, if you have created a class called protobuf.Vec3fProtos.Vec3f as an counterpart to the Vec3f of InstantIO, it is necessary to tell venice.lib, how it can transfer the values.
 
This is done by XML file.  See match.xml in the venice.lib main directory as an example.  The structure is the following:

    <matches>
      <match from="..." to="...">
        ...
      </match>
    </matches>

The "from" attribute and the "to" attribute in the "match" element are defining what source class is matched to what target class.  This has to be done for each of the two directions.  In case of matching RSB and IIO, the relation between the two cases is NOT symmetric.

Example Float:

You have created an protobuf class named MyFloat, which contains a single float value.  You want to match it to the java native Float (java.lang.Float), because Floats can be send over InstandIO out-slots.

    <match from="protobuf.MyFloatProtos.MyFloat" to="java.lang.Float">
      <constructor parameter="float"/>
      <getter name="getValue"/>
    </match>

The constuctor element tells venice.lib, that the target class can be contructed with a public constructor.  venice.lib assumes, that the constructor has the same name as the target class (according to java rules).  It also tells, that this constructor takes one parameter of the primitive type float.

The getter element tells venice.lib, that the float value of the protobuf class can be retrieved with the method getValue().

The other direction don't work this way, because there are no public constructors for protobuf classes.  Instead there are builders.  venice.lib will automatically use builders, if the target class is a protobuf class.  In this case a <methodpair> element is needed:

    <match from="java.lang.Float" to="protobuf.MyFloatProtos.MyFloat">
      <methodpair getter="floatValue" setter="setValue" type="float"/>
    </match>

The getter attribute tells venice.lib how to get the value from the Float.  The setter attribute tells venice.lib how to set the value for your target class.  venice.lib will then instanciate the builder class for this protobuf class, call the setter method and then commands the builder to build the instance.


Example for multifield classes:

Let's say, your protobuf class from the last example is now a multifield, so it can store multiple float values.  It is not possible to just take the singlefield class and make an array out of it, like you would do with java.lang types.  For protobuf you have to create a complete new class.  Let's call it protobuf.MyMultiFloatProtos.MyMultiFloat.

To match this to a Float array:

    <match from="protobuf.MyMultiFloatProtos.MyMultiFloat" to="[Ljava.lang.Float;" repeated="true">
      <constructor parameter="float"/>
      <getter name="getValue"/>
    </match>

The most important difference is the attribute repeated="true".  The name of the Float array follows java naming conventions (wrapping into "[L" and ";").  The rest is the same.

The other direction:

    <match from="[Ljava.lang.Float;" to="protobuf.MyMultiFloatProtos.MyMultiFloat" repeated="true">
      <methodpair getter="floatValue" setter="addValue" type="float"/>
    </match>

The setter method is here "addValue" - this is an important difference between singlefield and multifield builder classes.  Singlefield builder classes are using the "set" notation, while multifield builder classes are using "add".


Example singlefield protobuf class with more than one value:

Let's say you have created a protobuf version of the Vec3f class of InstantIO.  It takes three float values.  They are named X, Y and Z.  So a match entry would look like this:

    <match from="protobuf.MyVec3fProtos.MyVec3f" to="org.instantreality.InstantIO.Vec3f">
      <constructor>
        <parameter type="float" index="0"/>
        <parameter type="float" index="1"/>
        <parameter type="float" index="2"/>
      </constructor>
      <getter name="getX" index="0"/>
      <getter name="getY" index="1"/>
      <getter name="getZ" index="2"/>
    </match>

For the constructor element we need now more information, so there are the parameter elements.  Each parameter element tells venice.lib of what primitive type the constructor parameter is, and the order by index (beginning with 0).

The getter elements now need the additional attribut "index", so venice.lib can tell, to what constructor parameter the value goes.

The other direction is easy:

    <match from="org.instantreality.InstantIO.Vec3f" to="protobuf.MyVec3fProtos.MyVec3f">
      <methodpair getter="getX" setter="setX" type="float"/>
      <methodpair getter="getY" setter="setY" type="float"/>
      <methodpair getter="getZ" setter="setZ" type="float"/>
    </match>

Example for a multifield protobuf class with more than one value per field:

Let's say you have created a multifield version of the above example:

    <match from="protobuf.MyMultiVec3fProtos.MyMultiVec3f" to="[Lorg.instantreality.InstantIO.Vec3f;" repeated="true">
      <constructor>
        <parameter type="float" index="0"/>
        <parameter type="float" index="1"/>
        <parameter type="float" index="2"/>
      </constructor>
      <getter name="getX" index="0"/>
      <getter name="getY" index="1"/>
      <getter name="getZ" index="2"/>
    </match>
   
And the other direction:
	
    <match from="[Lorg.instantreality.InstantIO.Vec3f;" to="protobuf.MyMultiVec3fProtos.MyMultiVec3f" repeated="true">
      <methodpair getter="getX" setter="addX" type="float"/>
      <methodpair getter="getY" setter="addY" type="float"/>
      <methodpair getter="getZ" setter="addZ" type="float"/>
    </match>


== XIOParser

The subclasses of XIOParser provides methods for parsing XIO lines into events and vice versa.  Use eventToString to parse an event into a XIO line.  Use stringToEvent to parse a XIO line into an event.  Use preparseTS to quickly parse only the timestamp from the line.

So far there are two subclasses:

* XIODomParser uses the java XML Dom parser.  Slow, but robust.
* XIORegExParser uses regular expression matching.  Fast.


== Additional classes

AbstractSlot: This should be used by applications to be independent from the network protocol.  An AbstractSlot can describe a slot for both, instantIO and RSB.

AbstractSlotListener: An application that wants to listen to instantIO or RSB should have a listener that extends this class and register it with setMasterInSlotListener.  Then venice.lib will call the newData method of that listener every time new data comes in from in-slots.


== How to build up the slot filtering sensor XML-file

Selfexplaning Example:

    <?xml version="1.0"?>
    <Sources>
    <Sensor name = "Facelab">
      <Namespace name  = "HeadData">
        <slot name = "frameNumber" type = "SFInt32"/>
        <slot name = "headConfidence" type = "SFFloat"/>
        <slot name = "position" type = "SFVec3f"/>
        <slot name = "rotation" type = "SFRotation"/>
        <slot name = "title" type = "SFString"/>
        <slot name = "switch A" type = "SFBool"/>
        <slot name = "object names" type = "MFString"/>
      </Namespace>
    </Sensor>
    </Sources>

_Hint_ for users of older versions of venice.ipc: The unique tag <Sources> is needed at top level, so enclose your old sensorfile in <Sources>...</Sources>.


== Known bugs

Unfortunately instantreality.jar does not support all datatypes. See the following table for data type support.

    Type     ! SF  ! MF  ! class name
    ---------+-----+-----+--------------------------------------
    Boolean  | yes | no  | java.lang.Boolean
    String   | yes | yes | java.lang.String
    Integer  | yes | no  | java.lang.Integer
    Long     | no  | no  | java.lang.Long
    Float    | yes | no  | java.lang.Float
    Double   | yes | no  | java.lang.Double
    Object   | no  | no  | java.lang.Object
    Vec2f    | yes | yes | org.instantreality.InstantIO.Vec2f
    Vec3f    | yes | yes | org.instantreality.InstantIO.Vec3f
    Rotation | yes | yes | org.instantreality.InstantIO.Rotation
