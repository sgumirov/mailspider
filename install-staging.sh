set -e

dir="/usr/share/MailSpider-staging"

version="1.14-staging"
echo Installing version: $version
echo To change version edit 'install.sh'

mvn clean compile assembly:single -Dmaven.test.skip=true -DskipTests

if [ ! -d "$dir" ]; then
  sudo mkdir $dir
  sudo chown mailspider:mailspider $dir
fi

#todo check for file existed and add flag to override/upgrade

./jarconfig.sh $version
cp MailSpider-$version-configs.jar $dir
cp target/MailSpider-$version-jar-with-dependencies.jar $dir
cp systemd-env.conf $dir
sudo cp mailspider-staging.service /etc/systemd/system/
sudo chown root:root /etc/systemd/system/mailspider-staging.service
sudo chmod 664 /etc/systemd/system/mailspider-staging.service
sudo systemctl daemon-reload
echo Installed successfully. Please switch to new service manually:
echo sudo systemctl enable mailspider-staging
echo sudo systemctl disable mailspider
echo sudo systemctl stop mailspider
echo sudo systemctl start mailspider-staging
echo 
echo In order to rollback:
echo ./rollback-from-staging.sh