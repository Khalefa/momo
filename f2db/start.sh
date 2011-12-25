#!/bin/sh
# Specify appropriate SWT library!

SWT_JAR=libs/ext/swt/swt-gtk-linux-x86_64.jar

JARS=$SWT_JAR
for f in libs/*.jar
do
    JARS=$JARS:$f
done

export MOZILLA_FIVE_HOME=/usr/lib/`rpm -q firefox | sed -e "s/\(firefox-[^-]*\)-.*/\1/"`
#export MOZILLA_FIVE_HOME=/usr/lib64/xulrunner-1.9.2.20/
export LD_LIBRARY_PATH=$MOZILLA_FIVE_HOME:$LD_LIBRARY_PATH

java -Xmx512M -classpath build/classes:$JARS -Djava.library.path=$SWT_JAR: Demo 


