#!/bin/bash
### BEGIN INIT INFO
# Provides: rainfall-store
# Required-Start: network
# Default-Start: 2 3 4 5
# Default-Stop: 0 1 6
# Description: Rainfall-store init script
### END INIT INFO
###########################################
#This script should be copied to
#/etc/init.d on http://localhost
#No additional configuration is required.

#Location of the fat jar file:
APP_PATH=${rainfall-store_PATH:-/data/rainfall-store}
echo "Deployment path: ${APP_PATH}"

#Log file
LOGFILE='rainfall-store.log'

function start_rainfall-store {
    #Fat jar file (must be exactly one in the directory):
    APP_JAR="rainfall-store-*-fat.jar"
    echo "Jar file: $(ls ${APP_JAR})"

    #Properties file to be found in the jar:
    APP_PROPS=${rainfall-store_PROPS:-prod.props}
    echo "Properties: ${APP_PROPS}"

    JAVA_PATH=${JAVA_HOME:-/jdk-zulu-1.8}

    echo "Java installation: $JAVA_PATH"
    echo "Starting rainfall-store"
    echo "Logging to ${LOGFILE}"

    ${JAVA_PATH}/bin/java \
     -jar \
    -Xmx1024m \
    -Xss1024k \
    -XX:MaxPermSize=512m \
    -XX:MaxDirectMemorySize=2560m \
    -DpropsFile=${APP_PROPS} \
    ${APP_JAR} &> ${LOGFILE} &
}

function stop_rainfall-store {
    echo "Stopping rainfall-store"
    fuser --kill ${LOGFILE}
}


#Enter the deployment dir
pushd ${APP_PATH} > /dev/null

#Check the command line arg
case $1 in

    start)
        start_rainfall-store
        ;;

    stop)
        stop_rainfall-store
        ;;

    restart)
        stop_rainfall-store
        start_rainfall-store
        ;;
esac

#Exit the deployment dir
popd > /dev/null

