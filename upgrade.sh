set -e
git pull
mvn clean compile assembly:single
cp target/MailSpider-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/share/MailSpider/
./jarconfig.sh
cp MailSpider-1.0-SNAPSHOT-configs.jar /usr/share/MailSpider/
systemctl restart mailspider
systemctl status mailspider