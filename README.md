# MailSpider

Camel-based extendable system for retrieving files from email, ftp and http.
The processing route has endpoints (ftp, http, email), plugins and output (now implemented via
HTTP POST with 'application/octet-stream' content type).

# Plugins

For plugins developer notes please refer to com.gumirov.shamil.partsib.plugins.Plugin interface 
for details (see javadoc or source).

# Email filtering syntax

The set of rules is interpreted in this way: IF ANY OF RULE IS TRUE THEN THE EMAIL IS RECEIVED.

Config entry looks like:
{
  "id":"rule_01",
  "header":"From",
  "contains":"@gmail.com"
}

Header takes the following values: From, Body, Subject. Please note!! Yes, it's Starting From Big Letter header name!
Contains MUST NOT contain a double-quote symbol.

# Configuration

Consists of the following files: 
- config.properties for general config, here some other config file names are specified.
- log4j2.properties - logging properties. Here one can enable DB logging.
The following file names are configured via config.properties:
- email_reject_rules.json - email filtering rules, regexp-based.
- plugins.json - plugins config
- endpoints.json - endpoints config

Example of configuration file config.properties with comments is below:

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
# locations for repeat-filters (lists of name-size pairs) for email and ftp.
idempotent.repo=tmp/idempotent_repo.dat
email.idempotent.repo=tmp/email_idempotent_repo.dat

