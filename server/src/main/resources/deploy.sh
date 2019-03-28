#!/usr/bin/env bash
set -o pipefail

#Fat jar file:
NEW_JAR="server/target/rainfall-store-*-fat.jar"

#Destination of the fat jar file:
APP_PATH='/data/perfstore'

function deploy_perfstore {
    echo "Deploying perfstore"
    date

    echo "Jar file: $(ls ${NEW_JAR})"

    echo "Deployment path: ${APP_PATH}"

    echo "Stopping perfstore"
    service perfstore stop

    echo "Deleting old jar."
    rm ${APP_PATH}/*.jar

    echo "Copying new jar"
    cp ${NEW_JAR} ${APP_PATH}

    echo "Copying perfstore script"
    cp server/target/classes/perfstore.sh /etc/init.d/perfstore

    echo "Starting perfstore"
    service perfstore start
}

deploy_perfstore 2>&1 | tee deploy.log