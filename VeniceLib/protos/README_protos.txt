How to compile a .proto file
============================
1. Template for the .protoc file:

    package protobuf;
    option java_package = "protobuf";
    option java_outer_classname = "XXXProtos";

    message XXX {
      // your values
    }

Where XXX is the name of your class. The name of the file has to be XXX.proto

2. Copy your .proto file into the folder one level above your protobuf folder.
Example:
Your .proto file is named Test.proto and your protobuf folder is home\testprogramm\protobuf. Then Test.proto is to be copied into home\testprogramm.

3. Compile it with protoc. If protoc is not recognized by your system, download it from google, copy it into the same folder (one level above protobuf).

    > protoc --java_out=. XXX.proto

This creates a XXXProtos.java in the protobuf folder.
This source file can already be used in your source code.
If used with a bin version, further compiling is neccessary, see (4).

4. Compile it with java. Change dir to the protobuf folder. Type

    > javac -cp .;..\protobuf-java-2.4.1.jar XXXProtos.java

This creates all class files. The application should recognize them now.