# MailSpider

Camel-based extendable system for retrieving files from email, ftp and http.
The processing route has endpoints (ftp, http, email), plugins and output (now implemented via
HTTP POST with 'application/octet-stream' content type).

# Version status

Version 1.1. An officially deployed at the customer installation.

# Scripts

Added systemd script called mailspider.service (see systemd docs on how to add service).

Added upgrade.sh which does the following (and if ANY step fails script stops!):
- git pull, compiles sources and builds single jar (without configs!)
- jars configs
- copies service and config jars into /usr/share/mailspider/
- restarts service (systemd) and prints status

```
set -e
git pull
mvn clean compile assembly:single
cp target/MailSpider-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/share/MailSpider/
./jarconfig.sh
cp MailSpider-1.0-SNAPSHOT-configs.jar /usr/share/MailSpider/
systemctl restart mailspider
systemctl status mailspider
```

# Plugins

For plugins developer notes please refer to com.gumirov.shamil.partsib.plugins.Plugin interface 
for details (see javadoc or source).

# Configuration

Consists of the following files: 
- config.properties - main config, here some other config file names are specified.
- log4j2.properties - logging properties. Here one can enable DB logging.
The following file names are configured via config.properties:
- email_reject_rules.json - email filtering rules, regexp-based.
- plugins.json - plugins config
- endpoints.json - endpoints config
- pricehook.tagging.config.filename - tagging with price hook ids

Example of configuration file config.properties with comments is below:
```
# this section enables or disables endpoint groups:
email.enabled=1
ftp.enabled=0
http.enabled=0
# debug only:
local.enabled=0
# http post output:
output.url=http://im.mad.gd/2.php
# references to other configs
endpoints.config.filename=target/classes/test_local_endpoints.json
email.rules.config.filename=target/classes/email_reject_rules.json
plugins.config.filename=target/classes/plugins.json
pricehook.tagging.config.filename=target/classes/email_tagging_rules.json
# locations for repeat-filters (lists of name-size pairs) for email and ftp.
idempotent.repo=tmp/idempotent_repo.dat
email.idempotent.repo=tmp/email_idempotent_repo.dat
# http post max size in bytes
max.upload.size=1024000
```

# Http post size split
The following headers are used to notice upload part number:
- X-Part - a zero-based index
- X-Parts-Total - total number of parts

```
HTTP/1.1 200 OK
POST
Content-Type: application/octet-stream
Content-Length: 1
X-Filename: test.bin
X-Pricehook: pricehookId
X-Part: 2
X-Parts-Total: 8
Transfer-Encoding: chunked
Host: im.mad.gd
Connection: Keep-Alive
User-Agent: Apache-HttpClient/4.3.4 (java 1.5)
Accept-Encoding: gzip,deflate
```

# Email filtering syntax

The set of rules is interpreted in this way: IF ANY OF RULE IS TRUE THEN THE EMAIL IS RECEIVED.

Single rule looks like:
```json
{
  "id":"rule_01",
  "header":"From",
  "contains":"@gmail.com"
}
```

Header takes the following values: From, Body, Subject. Please note!! Yes, it's Starting From Big Letter header name!
Contains MUST NOT contain a double-quote symbol.

# Plugins config

Array of fully qualified class names, executed in order specified in config:
```json
[
  "com.gumirov.shamil.partsib.plugins.NoOpPlugin",
  "com.gumirov.shamil.partsib.plugins.NoOpPlugin"
]
```

# Endpoints config

- id - is used everywhere in log to track source of message
- url - address of source, see examples. Please note of url format (no imap:// in email)
- user and pwd - self-explainory
- delay - period of pull
- factory - fully qualified class name, used ONLY for http endpoint. MUST BE USED for http. Purpose is maintaining the 
procedure of logging in to web portals, see code for details. Actually this is needed because there's no universal of 
logging in to different web sites, so we need to use the specific implementation for every http endpoint.

```json
{
  "ftp":[
    {
      "id":"ftp_supplier_dir",
      "url":"ftp://127.0.0.1/test/",
      "user":"anonymous",
      "pwd":"a@google.com",
      "delay":"60000"
    }
  ],
  "http":[
    {
      "id": "HTTP_Optima",
      "factory": "com.gumirov.shamil.partsib.factories.OptimaRouteFactory",
      "url": "https://optma.ru/index.php?r=site/products",
      "user": "partsib",
      "pwd": "partsib5405",
      "delay": "60000"
    }
  ],
  "email":[
    {
      "id":"email_inbox_mailru_01",
      "url":"imap.mail.ru",
      "user":"username",
      "pwd":"password",
      "delay":"60000"
    }
  ]
}
```

# Price hook ids tagging

To send source pricehook id (one only) to the output the config the set of rules is used with the syntax similar to email filtering config:
```json
[
  {
    "id":"rule_01",
    "header":"From",
    "contains":"rossko",
    "supplierid":"10"
  },
  {
    "id":"rule_01",
    "header":"From",
    "contains":"rossko1",
    "supplierid":"11"
  }
]
```

# Unit tests

Unit tests are OK to run any time with 'mvn tests' command.
Please note that _integration_ (AT) tests require a real FTP/Email to be prepared. Refer to source code, they are not 
documented anywhere: EmailRouteTest, FTPRouteTest. Integration tests are excluded from execution using @Ignore.

