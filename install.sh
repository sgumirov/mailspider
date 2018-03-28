set -e

dir="/usr/share/MailSpider"
version="1.9"
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
echo Installed successfully
