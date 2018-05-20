# MailSpider

Camel-based extendable system for retrieving files from email, ftp and http.
The processing route has endpoints (ftp, http, email), plugins and output (now implemented via
HTTP POST with 'application/octet-stream' content type).

# Version status and important changes
See below some version-specific details

- (AT: special chars) 1.11-pre: Added automation test for special chars in mail filter
- Version 1.10: Multiple simultaneous email endpoints AT added.
- Version 1.9: Lots number of small changes related to Plugins API, plugins pipeline and AT. Javadocs added.
- Version 1.8: Added features: periodic notifications, old mail delete (30 days). Added plugins cleanup. Automatic tests improved.
- Version 1.7: Moved to new maven dependencies (extracted mailspider-base, added test runtime dependency)
- Version 1.6a: Incompatible changes: extracted plugin-related interfaces into separate project (Mailspider-Base). Added EncodingTest.
- Version 1.5: Added support for quotes in rules. Added 'filerules' for tagging separate attachments. 
AT refactored, added base class: now AT is easy-to-use framework, see YahooRawMailTest class for example.
- Version 1.4. Important Camel bug fixed: 'Bare attachment' issue. When mail message has no body but a single attachment on 
the root level (without multipart), then Camel fails to extract attachment. This release fixes this. 
AT for this case: EmailNestedMessageTest.testBareAttachmentIssue(). Tested is against 'issue.eml'.
- Version 1.3. Changed name of email accept rules (".accept" added): email.accept.rules.config.filename, multiple options added, 
 added retries removed in case of output endpoint failure.
-- Now loading pricehook config from network, see below in 'Pricehook IDs tagging config loading from network'
- Version 1.2. Deployed with pricehook tagging.
- Version 1.1. An officially deployed at the customer installation.

### Changes in 1.11

Automation tests are now able to verify tags used for each attachment. Added 2 new tests for BERG-related tags issues 
(special chars and raw emails against prod tag rules).

### Known issue (as of 1.10)

Delete old mail date parser can fail on some formats, effectively disabling this route of mailbox cleanup.

Readme polished, details added.

### Changes in 1.9

Added option of passing on pipeline when plugin throws an exception. Previously when plugin thrown an exception the pipeline
just ignored that. Now there's an option to stop pipeline message passing on plugin exception. Configured by the following 
config key, default value false:
```
plugin.pass.when.error=false
```

# Delete old mail

Purges old mail from email account.
Configured in main config by properties:
```
delete_old_mail.enabled=true
#how many days to keep
delete_old_mail.keep.days=30
#period of check for mail to delete in hours (recommended value: 24
delete_old_mail.check_period.hours=24
```

# Notifications

Added in: 1.8

MailSpider sends email to specified address once in period when processing is in progress. If no emails are processed
then notifications are stopped. This could be in case of no incoming email (which is fine) or something is wrong like
no access to email or MailSpider process is dead for some of the reasons.

New config file notification.properties is required now. Structure is following:
```
notification.period=300000
email.from=partsibprice@yahoo.com
email.to=partsibprice@yahoo.com
email.uri=smtps://smtp.mail.yahoo.com:465?username=partsibprice@yahoo.com&password=xxxxxxxxx&debugMode=true
```

Main config requires location of notification config now:
```
#notification config locations
notification.config=notification.properties
```

# Messages tracing in log

Camel tracing option is managed by "tracing" boolean config value. Tracing is enabled if not specified (this to be changed in
2.0 assuming will to stabilize and more specific log error reporting).

# Installation

1. Make sure user and group called mailspider exists.

2. Add systemd script called mailspider.service (see systemd docs on how to add service).

3. Run install.sh to install/upgrade (warning: overrides existing service JAR and config JAR).

# Logging

Currently logging is done via Log4j2 and logs are set to output to both console and file. See 
configuration/log4j2.properties.

# Plugins

Note: interface is defined in Mailspider-Base-VERSION maven-loaded module. See version in dependencies section of pom.xml.

For plugins developer notes please refer to 'com.gumirov.shamil.partsib.plugins.Plugin' interface 
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

Example of configuration file config.properties with comments is below. Please see current version git's config.properties:
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

# Output: HTTP post size partitioning

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
a list of comma-separated extensions without dots. See example below. To disable filtering set an empty value.
Example:
```
file.extension.accept.list=xls,xlsx,csv,txt
```

# Email filtering ('accept') rules syntax

The set of rules is interpreted in this way: IF ANY OF RULE IS TRUE THEN THE EMAIL IS ACCEPTED. The config file is loaded
 from file with filename specified in main config's key 'email.accept.rules.config.filename'.

Rule list example (one rule in list here):
```json
[
  {
    "id": "rule_01",
    "header": "From",
    "contains": "@gmail.com",
    "ignorecase": "true"
  }
]
```
Parameter 'id' is for logger, so it's better to set ids different values.
If parameter 'ignorecase' is set to 'true' or '1' then 'contains' field value is compared ignoring case.
Parameter 'contains' can have double-quote symbol escaped with '\"'. See section 'Attachment tagging' below for example.
Parameter 'header' is one of the following values: 'From', 'Subject'. Please note: YES, it DOES start From Big 
Letter!

# Pricehook IDs tagging

Pricehook ID is a tag send with each file sent via output. It could be either one per email message or per-attachment 
ID (see below 'Attachment tagging' section for this).

Output HTTP POST header name for ID is: ```X-Pricehook```

The network loading of this config is done via http every time the email message is processed and the same set of
rules are applied to all the files attached to this email.

Note that double-quotes could be escaped with backslash: '\"'.

## Pricehook IDs tagging config loading from network

Network url is specified in main config under parameter named 'pricehook.config.url'. Note that network rules
replaces local config in sense of 'if at least one tagging rule is load from network, local tagging rules are
dismissed for camel exchange'.

## Attachment tagging

To tag attachments separately (email ID will be overwritten if filename matches).
See section 'filerules' in example below. Note that double-quotes could be escaped with backslash: '\"'.

```json
[
  {
    "id": "rule_01",
    "header": "From",
    "contains": "rossko",
    "pricehookid": "10",
    "filerules": [
      {
        "namecontains": "_NSK_1",
        "pricehookid": "10.1"
      },
      {
        "namecontains": "_NSK_2",
        "pricehookid": "10.2"
      }
    ]
  },
  {
    "id": "rule_02",
    "header": "Subject",
    "contains": "test123 \"quoted\"",
    "pricehookid": "11"
  }
]
```


# Plugins config

Array of fully qualified class names. Plugins are executed in same order as specified in config. Example is below:
```json
[
  "com.gumirov.shamil.partsib.plugins.NoOpPluginFirst",
  "com.gumirov.shamil.partsib.plugins.NoOpPluginSecond"
]
```

# Endpoints config

- id - is used everywhere in log to track source of message
- url - address of source, see examples. Please note of url format: PROTOCOL://HOST:PORT. For email protocol by default 
'imaps://' is used (note 's' for SSL)', could also be imap:// for non-SSL, pop3:// or pop3s://. 
See also the config's parameter 'default.email.protocol' which could change the default behaviour.  
- user, pwd - self-explainory
- delay - period of pull in msec
- parameters - map of key-value pairs to pass to camel endpoint. Use with care. Notable example is "delete=true" 
parameter needed for pop3 to work fine.
- factory - fully qualified class name, used ONLY for http endpoint. MUST BE USED for http. Purpose is maintaining the 
procedure of logging in to web portals, see Factory code for details. Actually this is needed because there's no universal of 
way to sign in to different web sites, so we need to use the specific implementation for every web site.

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
      "pwd": "partsibPASSWORD",
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


# Tests

All tests are designed to run any time with 'mvn tests' command. They all MUST pass at any time. Do not run them parallel
same time same computer (as they create local endpoints). 
Note that if any of ports are bound tests fails, so in case of tests failing check netstat -anl to see if ports needed 
are taken. See list of ports below.
Integration tests that require external setup are excluded from execution using @Ignore (basically they are the 
development ones). 

## Local portsd used for automated tests

8143, 8080, 1993, 1143, 3143, 3993
