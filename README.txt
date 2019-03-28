Rainfall-store is a data store and a web application
for storing, analysing and visualising performance testing data.

The web application is available at
http://localhost:4567/performance

Before storing test results, register the test
by submitting its name and description using the form in the web page above.

Then integrate the test code using client side API described here:
client/src/main/asciidocs/ClientAPI.txt

The REST API is described here:
server/src/main/asciidocs/PerfController.txt

The data model is described here:
server/src/main/asciidocs/rainfall-store.txt

The source code of Rainfall-store is in this repository:
https://github.com/aurbroszniowski/Rainfall-store

Rainfall-store web application is currently located at
localhost in /data/rainfall-store

Rainfall-store gets re-deployed automatically every time a PR is merged
to its master branch by Jenkins running this script:
server/src/main/resources/deploy.sh
The deploy.log is then shown in the Jenkins master build page.


