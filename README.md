# MailSpider

Camel-based extendable system for retrieving files from email, ftp and http.
The processing route has endpoints (ftp, http, email), plugins and output (now implemented via
HTTP POST with 'application/octet-stream' content type).

# Version status and important changes

Version 1.3. [IMPORTANT] Changed name of email accept rules (".accept" added): email.accept.rules.config.filename, multiple options added, 
             added retries in case of output endpoint failure.
Version 1.2. Deployed with pricehook tagging.
Version 1.1. An officially deployed at the customer installation.

# Messages tracing in log

Camel tracing option is managed by "tracing" boolean config value. Tracing is enabled if not specified (this to be changed in
2.0 assuming will to stabilize and more specific log error reporting).

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

# Logging

Currently logging is done via Log4j2 and logs are set to output to both console and file. See 
configuration/log4j2.properties.

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
# tracing is Camel Tracing, by default true if not specified
tracing=true
# this section enables or disables protocol routes:
email.enabled=1
ftp.enabled=0
http.enabled=0
# route from local files, debug only:
local.enabled=0
# default email protocol (name only without '://'). Used in case when no protocol is specified  
default.email.protocol=imaps
# http post output:
output.url=http://im.mad.gd/2.php
# references to other configs
endpoints.config.filename=target/classes/test_local_endpoints.json
email.accept.rules.config.filename=target/classes/email_reject_rules.json
plugins.config.filename=target/classes/plugins.json
pricehook.tagging.config.filename=target/classes/email_tagging_rules.json
# locations for repeat-filters (lists of name-size pairs) for email and ftp.
idempotent.repo=tmp/idempotent_repo.dat
email.idempotent.repo=tmp/email_idempotent_repo.dat
# http post max size in bytes
max.upload.size=1024000
# pricehook id tagging rules url
pricehook.config.url=http://localhost/email_tagging_rules.json
#comma-separated list of extensions to accept (after unpack)
file.extension.accept.list=xls,xlsx,csv,txt

```

# Http post size partitioning

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

# Extension filtering

The file extension list to accept is defined in config parameter 'file.extension.accept.list' and takes 
a list of comma-separated extensions without dots. See example. 
To disable set empty value. 

# Email filtering syntax

The set of rules is interpreted in this way: IF ANY OF RULE IS TRUE THEN THE EMAIL IS ACCEPTED.

Single rule looks like:
```json
{
  "id":"rule_01",
  "header":"From",
  "contains":"@gmail.com",
  "ignorecase":"true"
}
```
Parameter 'id' is for logging, so it's better to set ids different values.
If parameter 'ignorecase' is set to 'true' or '1' then 'contains' field value is compared ignoring case.
Parameter 'contains' MUST NOT contain a double-quote symbol.
Parameter 'header' is one of the following values: 'From', 'Subject'. Please note: yep, it DOES start From Big Letter header name!

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
- url - address of source, see examples. Please note of url format: PROTOCOL://HOST:PORT. For email protocol by default 
'imaps://' is used (note 's' for SSL)', could also be imap:// for non-SSL, pop3:// or pop3s://. 
See also the config's parameter 'default.email.protocol' which could change the default behaviour.  
- user and pwd - self-explainory
- delay - period of pull in msec
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

# Pricehook IDs tagging

The network loading of this config is done via http every time the email message is processed and the same set of
rules are applied to all the files attached to this email.

Network url is specified in main config under parameter named 'pricehook.config.url'. Note that network rules
replaces local config in sense of 'if at least one tagging rule is load from network, local tagging rules are
dismissed for exchange'.

To send source pricehook id (one per file) to the output the set of rules is used with the syntax similar to
email filtering config. See example below.
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

