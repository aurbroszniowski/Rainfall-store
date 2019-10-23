#!/bin/bash


APP_JAR=${1}

shift
host=${1}
shift
port=${1}
shift
schema=${1}

URL="jdbc:mysql://${host}:${port}/${schema}?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC"

shift
USERNAME=${1}

shift
PASSWORD=${1}

JAVA_PATH=${JAVA_HOME:-/jdk-zulu-1.8}

echo "Java installation: $JAVA_PATH"
echo "Full database URL: ${URL}"
echo "Database username: ${USERNAME}"

${JAVA_PATH}/bin/java -jar \
 -Dspring.profiles.active=prod \
 -Dspring.datasource.url=${URL} \
 -Dspring.datasource.username=${USERNAME} \
 -Dspring.datasource.password=${PASSWORD} \
 -Xmx1024m \
 -Xss1024k \
${APP_JAR}
