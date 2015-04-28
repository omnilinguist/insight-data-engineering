#!/usr/bin/env bash

# Use existing Java/Scala on system or download them if not found. Versions of each are the latest stable releases as of 2015/04/24.

echo "Looking for Java..."
which java > /dev/null 2> /dev/null
if [ $? -ne 0 ]
then
  echo "Did not find Java, downloading...!"

  # Java download curl command obtained from https://gist.github.com/P7h/9741922
  BASE_URL=http://download.oracle.com/otn-pub/java/jdk/8u45-b14/jdk-8u45
  JDK_VERSION=${BASE_URL: -8}

  wget -c -O "$JDK_VERSION-linux-x64.tar.gz" --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" "${BASE_URL}-linux-x64.tar.gz"
  tar xzvf "$JDK_VERSION-linux-x64.tar.gz"
  export JAVA_PATH=`pwd`/jdk1.8.0_45/bin/java
else
  echo "Found Java!"
  export JAVA_PATH=`which java`
fi

echo "Looking for Scala..."
(which scalac && which scala) > /dev/null 2> /dev/null
if [ $? -ne 0 ]
then
  echo "Did not find Scala, downloading..."
  wget http://downloads.typesafe.com/scala/2.11.6/scala-2.11.6.tgz
  tar xzvf scala-2.11.6.tgz
  export SCALAC_PATH=`pwd`/scala-2.11.6/bin/scalac
  export SCALA_PATH=`pwd`/scala-2.11.6/bin/scala
else
  echo "Found Scala!"
  export SCALAC_PATH=`which scalac`
  export SCALA_PATH=`which scala`
fi

echo ""
echo "Using Java at $JAVA_PATH. Current Java version:"
$JAVA_PATH -version
echo ""
echo "Using Scala at $SCALA_PATH. Current Scala version:"
$SCALA_PATH -version

echo ""
echo "Compiling program from src/ProcessText.scala..."
$SCALAC_PATH src/ProcessText.scala
echo "Running program..."
$SCALA_PATH ProcessText wc_input wc_output
