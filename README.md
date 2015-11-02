Dropwizard GELF
===============
[![Build Status](https://travis-ci.org/gini/dropwizard-gelf.svg?branch=master)](https://travis-ci.org/gini/dropwizard-gelf)
[![Coverage Status](https://img.shields.io/coveralls/gini/dropwizard-gelf.svg)](https://coveralls.io/r/gini/dropwizard-gelf)
[![Maven Central](https://img.shields.io/maven-central/v/et.gini.dropwizard/dropwizard-gelf.svg)](http://mvnrepository.com/artifact/net.gini.dropwizard/dropwizard-gelf)

Addon for Dropwizard adding support for logging to a GELF-enabled server like [Graylog](https://www.graylog.org/)
or [logstash](http://logstash.net/) using the [GELF appender for Logback](https://github.com/Moocar/logback-gelf).


Usage
-----

The Dropwizard GELF provides an `AppenderFactory` which is automatically registered in Dropwizard and will send log
messages directly to your configured GELF-enabled server.


Logging startup errors to Graylog
---------------------------------

In order to log startup errors (i. e. before the `GelfAppenderFactory` has been properly initialized) to a GELF-enabled
server, the Dropwizard application has to run `GelfBootstrap.bootstrap()` in its `main` method and set a custom
`UncaughtExceptionHandler` for the main thread.

    public static void main(String[] args) throws Exception {
        GelfBootstrap.bootstrap(NAME, GELF_HOST, GELF_PORT, false);
        Thread.currentThread().setUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());

        new MyDropwizardApplication().run(args);
    }


Configuration
-------------

The Logback GELF appender can be configured using the provided `GelfConfiguration` class which basically mirrors the
appender configuration outlined in [logback-gelf README](https://github.com/Moocar/logback-gelf/blob/master/README.md).

Your YAML configuration could include the following snippet to configure the `GelfLoggingBundle`:

    appenders:
      - type: console
      - type: gelf
        host: localhost
        # facility: MyApplication
        # threshold: ALL
        # host: localhost
        # port: 12201
        # useLoggerName: true
        # useThreadName: true
        # useMarker: false
        # serverVersion: 0.9.6
        # chunkThreshold: 1000
        # messagePattern: %m%rEx
        # shortMessagePattern: %.-100(%m%rEx)
        # hostName: hostname
        includeFullMDC: true
        additionalFields:
          userName: _userName
        staticAdditionalField:
          _node_name: www013
        fieldTypes:
          _request_id: long


Properties
----------

* **enabled**: Specify if logging to a GELF-compatible server should be enabled. Defaults to false;
* **facility**: The name of the application. Appears in facility column in the Graylog web interface. Defaults to the application name;
* **host**: The hostname of the Graylog server to send messages to. Defaults to "localhost";
* **port**: The port of the Graylog server to send messages to. Defaults to 12201;
* **useLoggerName**: If true, an additional field call "_loggerName" will be added to each GELF message. Its contents will be the fully qualified name of the logger. e. g. `com.company.Thingo`. Defaults to true;
* **useThreadName**: If true, an additional field call "_threadName" will be added to each GELF message. Its contents will be the name of the thread. Defaults to true;
* **serverVersion**: Specify which version the Graylog server is. This is important because the GELF headers changed from 0.9.5 -> 0.9.6. Allowed values = 0.9.5 and 0.9.6. Defaults to "0.9.6";
* **chunkThreshold**: The maximum number of bytes allowed by the payload before the message should be chunked into smaller packets. Defaults to 1000;
* **useMarker**: If true, and the user has used an [SLF4J marker](http://slf4j.org/api/org/slf4j/Marker.html) in their log message by using one of the marker-overloaded [log methods](http://slf4j.org/api/org/slf4j/Logger.html), then the `marker.toString()` will be added to the GELF message as the field `_marker`.  Defaults to false;
* **messagePattern**: The layout of the actual message according to [PatternLayout](http://logback.qos.ch/manual/layouts.html#conversionWord). Defaults to "%m%rEx";
* **shortMessagePattern**: The layout of the short message according to [PatternLayout](http://logback.qos.ch/manual/layouts.html#conversionWord). Defaults to none which means the message will be truncated to create the short message;
* **hostName** The sending host name. Used to override the name of the server which will appear in the log messages. Defaults to the output of 'hostname';
* **includeFullMDC**: Add all fields from the MDC will be added to the GELF message. If set to false, only the keys listed in additionalFields will be added to a GELF message. Defaults to false;
* **additionalFields**: Add additional fields filled from the [MDC](http://logback.qos.ch/manual/mdc.html). The key is the key of the MDC, the value is the key inside the GELF message. Defaults to empty;
* **staticAdditionalFields**: Add static additional fields. Defaults to empty;
* **fieldTypes**: Add type information to additional fields. Valid types:`int`, `long`, `float`, `double`. Defaults to `string`;


Maven Artifacts
---------------

This project is available on Maven Central. To add it to your project simply add the following dependencies to your POM:

    <dependency>
      <groupId>net.gini.dropwizard</groupId>
      <artifactId>dropwizard-gelf</artifactId>
      <version>0.9.0-2</version>
    </dependency>


Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/gini/dropwizard-gelf/issues).


Acknowledgements
----------------

Thanks to Nick Telford for his [initial version](https://gist.github.com/dd5e000c3327484540a8) of the `GraylogBundle`.


Contributors
------------

* [Daniel Scott](https://github.com/danieljamesscott)
* [Michal Svab](https://github.com/msvab)


License
-------

Copyright (c) 2012-2013 smarchive GmbH, 2013-2015 Gini GmbH, 2015 Jochen Schalanda

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the LICENSE file in this repository for the full license text.
