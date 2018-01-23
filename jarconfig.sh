#!/bin/bash
set -e
if [ ! -n "$1" ]
  then
    echo No args passed. Usage: ./jarconfig.sh {VERSION}
    exit -1
fi
if [ ! -d "config-partsib" ]; then
  echo "No config dir found: expected ./config-partsib"
  exit -1
fi
cd config-partsib
jar cvf ../MailSpider-$1-configs.jar *
cd ..