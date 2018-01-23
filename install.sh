set -e

dir="/usr/share/MailSpider"
version="1.7"
echo Installing version: $version
echo To change version edit 'install.sh'

if [ ! -d "$dir" ]; then
  sudo mkdir $dir
  sudo chown mailspider:mailspider $dir
fi

./jarconfig.sh $version
cp MailSpider-$version-configs.jar $dir
cp target/MailSpider-$version-jar-with-dependencies.jar $dir
echo Installed successfully
