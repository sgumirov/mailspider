set -e
git pull
mvn clean compile assembly:single
cp target/MailSpider-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/share/MailSpider/
./jarconfig.sh
cp MailSpider-1.0-SNAPSHOT-configs.jar /usr/share/MailSpider/
systemctl restart mailspider
systemctl status mailspider


#!/bin/bash

set -e
FILE=target/MailSpider-1.3-SNAPSHOT-jar-with-dependencies.jar

git pull
mvn clean compile assembly:single

if [ -f $FILE ]; then
  ./jarconfig.sh
  cp $FILE /usr/share/MailSpider/MailSpider-jar-with-dependencies.jar
  cp MailSpider-configs.jar /usr/share/MailSpider/
  echo "Restarting service"
  systemctl restart mailspider
  systemctl status mailspider
else 
  echo "JAR $FILE not found. Check version?"
fi
