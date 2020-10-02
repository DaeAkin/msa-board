#!/bin/sh
echo "********************************************************"
echo "Starting board "
echo "********************************************************"
#TODO Dspring=docker
java -jar -Dspring.profiles.active=docker /usr/local/board/board-0.0.1-SNAPSHOT.jar