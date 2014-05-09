#!/bin/sh
#
# Small shell script to show how to start/stop XProclet using jsvc
#
# Adapt the following lines to your configuration
JAVA_HOME=/usr/lib/jvm/java-openjdk/
XPROCLET_HOME=/home/xproclet/server
XPROCLET_CONF=$XPROCLET_HOME/conf/web.xml
DAEMON_HOME=$XPROCLET_HOME/jsvc
XPROCLET_USER=xproclet

# for multi instances adapt those lines.
TMP_DIR=/var/tmp
PID_FILE=/var/run/xproclet.pid

XPROCLET_OPTS=""
CLASSPATH=\
$JAVA_HOME/lib/tools.jar:\
$XPROCLET_HOME/jsvc/commons-daemon.jar:\
$XPROCLET_HOME/xproclet-server.jar

case "$1" in
  start)
    #
    # Start Atomojo
    #
    $DAEMON_HOME/jsvc \
    -user $XPROCLET_USER \
    -home $JAVA_HOME \
    -Djava.io.tmpdir=$TMP_DIR \
    -wait 10 \
    -pidfile $PID_FILE \
    -outfile $XPROCLET_HOME/server.out \
    -errfile '&1' \
    $XPROCLET_OPTS \
    -cp $CLASSPATH \
    org.xproclet.server.Daemon \
    $XPROCLET_CONF
    #
    # To get a verbose JVM
    #-verbose \
    # To get a debug of jsvc.
    #-debug \
    exit $?
    ;;

  stop)
    #
    # Stop Atomojo
    #
    $DAEMON_HOME/jsvc \
    -stop \
    -pidfile $PID_FILE \
    org.xproclet.server.Daemon
    exit $?
    ;;

  *)
    echo "Usage server.sh start/stop"
    exit 1;;
esac
