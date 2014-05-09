#!/bin/sh
dir=`dirname $0`
java -cp $dir/dist/xproclet-server.jar:$dir/build/test/classes/ org.xproclet.server.Main $*
