#!/usr/bin/env bash
set -o pipefail

#Fat jar file:
NEW_JAR="server/target/rainfall-store-*-fat.jar"

#Destination of the fat jar file:
APP_PATH=${rainfall_store_path:-/data/rainfall-store}

SOURCE_PATH=server/target/classes

function deploy_perfstore {
    echo "Deploying perfstore"
    date

    echo "Jar file: $(ls ${NEW_JAR})"

    echo "Deployment path: ${APP_PATH}"
    mkdir -p "${APP_PATH}"

    echo "Stopping perfstore"
    service perfstore stop

    echo "Deleting old jar."
    rm -f ${APP_PATH}/*.jar

    echo "Copying new jar and files"
    cp ${NEW_JAR} ${SOURCE_PATH}/*.props ${SOURCE_PATH}/*.xml ${APP_PATH}

    echo "Copying perfstore script"
    cp ${SOURCE_PATH}/perfstore.sh /etc/init.d/perfstore

    echo "Starting perfstore"
    service perfstore start
}

deploy_perfstore 2>&1 | tee deploy.log