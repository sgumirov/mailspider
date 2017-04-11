curl -X POST \
  'http://partsib.ru:8080/pss/client?p=pricelist.ImportFileHooked' \
  -H 'Cache-Control: no-cache' \
  -H 'Content-type: application/octet-stream' \
  --data-binary "@test.sh" \
  --verbose