```diff
@@ ===================================================================================== @@
@@ Mailspider is not actively maintained (security patches will continue to be applied). @@
@@                                                                                       @@
@@ Note: it's better (and probably cheaper) to run similar pipeline to extract data from @@
@@ main and web on AWS Glue or Step Functions with Lambda instead.                       @@
@@ ===================================================================================== @@
```
---

# Mailspider

Configurable gmail-IMAP-compatible ETL to periodically load file/attachment, extract, convert into CSV
(external plugins) and store into DB. It's designed to be extendable at any step. Opensource (see below
for license details).

**Data sources**: IMAP, POP3, FTP, HTTP.  
**Processing**: zip|rar ( xlsx, xls ) -> csv. Implemented in plugins.  
**Output**: HTTP upload.

![Overview](overview.png)

If you need a new feature, bugfix or any licensing-related questions, feel free to 
[contact me](https://shamil.gumirov.org/about/about). I'm open to a wide range of collaboration.

## Version history

See [changelist](CHANGES.md).

## Architecture diagrams and details

High-level email processing diagram (for loading via HTTP there're other loading steps):  
![High level processing](processing-diagram.png)

Detailed components diagram:  
![Detailed component](highlevel.png)

# License

Sources are available under GNU AGPL 3.0. See [LICENSE](LICENSE)

# Copyright

(C) 2016-2020 by Shamil Gumirov <shamil (dot) g1@gmail (dot) com>.
