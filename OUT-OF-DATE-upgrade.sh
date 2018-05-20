#!/bin/bash

set -e
FILE=target/MailSpider-1.4-SNAPSHOT-jar-with-dependencies.jar

runuser -u mailspider git pull
runuser -u mailspider mvn clean compile assembly:single

if [ -f $FILE ]; then
  runuser -u mailspider ./jarconfig.sh
  runuser -u mailspider cp $FILE /usr/share/MailSpider/MailSpider-jar-with-dependencies.jar
  runuser -u mailspider cp MailSpider-configs.jar /usr/share/MailSpider/
  echo "Restarting service"
  systemctl restart mailspider
  systemctl status mailspider
else 
  echo "JAR $FILE not found. Check version?"
fi
