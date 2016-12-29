package com.gumirov.shamil.partsib;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.GzipDataFormat;

/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

    /**
     * Let's configure the Camel routing rules using Java code...
     */
    public void configure() {

        // here is a sample which processes the input files
        // (leaving them in place - see the 'noop' flag)
        // then performs content based routing on the message using XPath
        /*from("file:src/data?noop=true")
            .choice()
                .when(xpath("/person/city = 'London'"))
                    .log("UK message")
                    .to("file:target/messages/uk")
                .otherwise()
                    .log("Other message")
                    .to("file:target/messages/others");*/
        /*from("file:/opt/ftp")
                .choice()
                .when(header("CamelFileName").contains(".gz")).unmarshal().gzip().log("gzipped").to("file:target/files")
                .when(header("CamelFileName").contains(".zip")).unmarshal().zip().log("zipped").to("file:target/files")
                .otherwise()
                .log("not gzipped")
                .to("file:target/files")
        ;*/
        from("imaps://sh.roller:gfhjkm12@imap.mail.ru?unseen=true&consumer.delay=10000&delete=false").
                log("new email");
    }

}
