# execute this file to install msgpack-rpc-0.7.0-SNAPSHOT.jar
#
# unfortunately this 3rd party library is not available online,
# so it has to be installed this way
#
mvn install:install-file -Dfile=./lib/msgpack-rpc-0.7.0-SNAPSHOT.jar -DgroupId=org.msgpack -DartifactId=msgpack-rpc -Dversion=0.7.0-SNAPSHOT -Dpackaging=jar