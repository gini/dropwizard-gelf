Dropwizard GELF Bundle
======================
[![Build Status](https://secure.travis-ci.org/gini/dropwizard-gelf.png?branch=master)](https://travis-ci.org/gini/dropwizard-gelf)

Addon bundle for Dropwizard to support logging to a GELF-enabled server like [Graylog2](http://graylog2.org/)
or [logstash](http://logstash.net/) using the [GELF appender for Logback](https://github.com/Moocar/logback-gelf).


Usage
-----

The Dropwizard GELF bundle consists of two parts, a [ConfiguredBundle](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/ConfiguredBundle.html)
and a [Servlet Filter](http://docs.oracle.com/javaee/6/api/javax/servlet/Filter.html) which can optionally be used to
send HTTP request logs to a GELF-enabled server.

To enable the `GelfLoggingBundle` simply add the following code to your [Service](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/Service.html)'s
[initialize method](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/Service.html#initialize%28com.yammer.dropwizard.config.Bootstrap%29):

    @Override
    public void initialize(Bootstrap<MyServiceConfiguration> bootstrap) {
        bootstrap.addBundle(new GelfLoggingBundle<MyServiceConfiguration>() {
                @Override
                public GelfConfiguration getConfiguration(MyServiceConfiguration configuration) {
                    return configuration.getGelf();
                }
            });
    }

You also need to add a field for `GelfConfiguration` to your own [Configuration](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/config/Configuration.html)
class.

In order to log HTTP requests being sent to your service you need to add the GelfLoggingFilter in the
[run method](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/Service.html#run%28T,%20com.yammer.dropwizard.config.Environment%29)
of your service:

    @Override
    public void run(MyServiceConfiguration configuration, Environment environment) {
        environment.addFilter(new GelfLoggingFilter(), "/*");
    }

The servlet filter adds a lot of data to the [Mapped Diagnostic Context (MDC)](http://logback.qos.ch/manual/mdc.html) which
can be used as additional fields, e. g. `remoteAddress`, `requestUri`, or `responseStatus`.


Configuration
-------------

The Logback GELF appender can be configured using the provided `GelfConfiguration` class which basically mirrors the
appender configuration outlined in [logback-gelf README](https://github.com/Moocar/logback-gelf/blob/master/README.md).

Your YAML configuration could include the following snippet to configure the `GelfLoggingBundle`:

    gelf:
      enabled: true
      # facility: MyService
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
        remoteAddress: _remoteAddress
        httpMethod: _httpMethod
        requestUri: _requestUri
        responseStatus: _responseStatus
        responseTimeNanos: _responseTime
      staticAdditionalField:
        _node_name:www013


Properties
----------

* **enabled**: Specify if logging to a GELF-compatible server should be enabled. Defaults to false;
* **facility**: The name of your service. Appears in facility column in the Graylog2 web interface. Defaults to "GELF";
* **host**: The hostname of the Graylog2 server to send messages to. Defaults to "localhost";
* **port**: The port of the Graylog2 server to send messages to. Defaults to 12201;
* **useLoggerName**: If true, an additional field call "_loggerName" will be added to each GELF message. Its contents
will be the fully qualified name of the logger. e.g: com.company.Thingo. Defaults to true;
*   **useThreadName**: If true, an additional field call "_threadName" will be added to each GELF message. Its contents
will be the name of the thread. Defaults to true;
* **serverVersion**: Specify which version the graylog2-server is. This is important because the GELF headers
changed from 0.9.5 -> 0.9.6. Allowed values = 0.9.5 and 0.9.6. Defaults to "0.9.6";
* **chunkThreshold**: The maximum number of bytes allowed by the payload before the message should be chunked into
smaller packets. Defaults to 1000;
* **useMarker**: If true, and the user has used an [SLF4J marker](http://slf4j.org/api/org/slf4j/Marker.html) in their
log message by using one of the marker-overloaded [log methods](http://slf4j.org/api/org/slf4j/Logger.html), then the
`marker.toString()` will be added to the GELF message as the field `_marker`.  Defaults to false;
* **messagePattern**: The layout of the actual message according to
[PatternLayout](http://logback.qos.ch/manual/layouts.html#conversionWord). Defaults to "%m%rEx";
* **shortMessagePattern**: The layout of the short message according to
[PatternLayout](http://logback.qos.ch/manual/layouts.html#conversionWord). Defaults to none which means the message will
be truncated to create the short message;
* **hostName** The sending host name. Used to override the name of the server which will appear in the log messages.
Defaults to the output of 'hostname'.
* **includeFullMDC**: Add all fields from the MDC will be added to the GELF message. If set to false, only the keys
listed in additionalFields will be added to a GELF message. Defaults to false;
* **additionalFields**: Add additional fields filled from the [MDC](http://logback.qos.ch/manual/mdc.html).  Defaults to empty;
* **staticAdditionalFields**: Add static additional fields. Defaults to empty;


Logging Jetty requests with GelfLoggingFilter
---------------------------------------------

If **includeFullMDC** is set to false, the desired fields must be added to the **additionalFields** section in the
service configuration in order to be logged as additional fields in the GELF messages. Otherwise only a string similar
(but not identical) to the NSCA request log format will be logged.

Additional MDC entries populated by `GelfLoggingFilter`:

* **remoteAddress**: The HTTP client's IP address
* **httpMethod**: The HTTP method in the request
* **protocol**: The HTTP protocol in the request (usually "HTTP/1.1")
* **requestAuth**:
* **userPrincipal**: The user name in the HTTP request
* **requestUri**: The URI in the HTTP request
* **requestLength**: The request size in bytes
* **requestContentType**: The `Content-Type` header of the HTTP request
* **requestEncoding**: The `Encoding` header of the HTTP request
* **userAgent**: The `User-Agent` header of the HTTP request
* **responseStatus**: The HTTP response status
* **responseContentType**: The HTTP response content type
* **responseEncoding**: The HTTP response encoding
* **responseTimeNanos**: The elapsed time in nano seconds
* **responseLength**: The length of the HTTP response


Logging startup errors to Graylog2
----------------------------------

In order to log startup errors (i. e. before the `GelfLoggingBundle` has been properly initialized) to a Graylog2-compatible server,
the Dropwizard service has to run `GelfBootstrap.bootstrap()` in its `main` method and set a custom `UncaughtExceptionHandler` for the
main thread.

    public static void main(String[] args) throws Exception {
        GelfBootstrap.bootstrap(NAME, GELF_HOST, GELF_PORT, false);
        Thread.currentThread().setUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());
        new MyService().run(args);
    }


Drawbacks
---------

Being implemented as a [ConfiguredBundle](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/ConfiguredBundle.html)
and a [Servlet Filter](http://docs.oracle.com/javaee/6/api/javax/servlet/Filter.html) (for logging HTTP requests)
dropwizard-gelf basically starts too late to catch all the startup log messages (e. g. from other bundles or before the
GelfLoggingBundle has been started) and duplicates a lot of functionality from Dropwizard's
[RequestLogHandlerFactory](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/config/RequestLogHandlerFactory.html).

On the long run the clean solution would be to add pluggable log handlers to Dropwizard in a way that won't break the
current abstraction.


Maven Artifacts
---------------

This project is available on Maven Central. To add it to your project simply add the following dependencies to your POM:

    <dependency>
      <groupId>net.gini.dropwizard</groupId>
      <artifactId>dropwizard-gelf</artifactId>
      <version>0.4.1</version>
    </dependency>


Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/gini/dropwizard-gelf/issues).


Acknowledgements
----------------

Thanks to Nick Telford for his [initial version](https://gist.github.com/dd5e000c3327484540a8) of the `GraylogBundle`.


Contributors
------------

* Daniel Scott (https://github.com/danieljamesscott)


License
-------

Copyright (c) 2012-2013 smarchive GmbH, 2013-2014 Gini GmbH

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the LICENSE file in this repository for the full license text.
