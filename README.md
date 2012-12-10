Dropwizard GELF Bundle
======================

Addon bundle for Dropwizard to support logging to a GELF-enabled server like [Graylog2](http://graylog2.org/)
or [logstash](http://logstash.net/) using the [GELF appender for Logback](https://github.com/Moocar/logback-gelf).

[![Build Status](https://secure.travis-ci.org/smarchive/dropwizard-gelf.png?branch=master)](https://travis-ci.org/smarchive/dropwizard-gelf)


Usage
-----

dropwizard-gelf consists of two parts, a [ConfiguredBundle](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/ConfiguredBundle.html)
and a [Servlet Filter](http://docs.oracle.com/javaee/6/api/javax/servlet/Filter.html) which can optionally be used to
send HTTP request logs to a GELF-enabled server.

To enable the GelfLoggingBundle simply add the following code to your [Service](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/Service.html)'s
[initialize method](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/Service.html#initialize%28com.yammer.dropwizard.config.Bootstrap%29):

    @Override
    public void initialize(Bootstrap<MyServiceConfiguration> bootstrap) {
        bootstrap.addBundle(new GelfLoggingBundle() {
                @Override
                public GelfConfiguration getConfiguration(MyServiceConfiguration configuration) {
                    return configuration.getGelfConfiguration();
                }
            });
    }

You also need to add a field for GelfConfiguration to your own [Configuration](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/config/Configuration.html)
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

The Logback GELF appender can be configured using the provided GelfConfiguration class which basically mirrors the
appender configuration outlined in [logback-gelf README](https://github.com/Moocar/logback-gelf/blob/master/README.md).

Your YAML configuration could include the following snippet to configure the GelfLoggingBundle:

    gelf:
      enabled: true
      facility: MyService
      # threshold: ALL
      # host: localhost
      # port: 12201
      # useLoggerName: true
      # useThreadName: true
      # serverVersion: 0.9.6
      # chunkThreshold: 1000
      # messagePattern = "%m%rEx";
      additionalFields:
        remoteAddress: _remoteAddress
        httpMethod: _httpMethod
        requestUri: _requestUri
        responseStatus: _responseStatus
        responseTimeNanos: _responseTime


Drawbacks
---------

Being implemented as a [ConfiguredBundle](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/ConfiguredBundle.html)
and a [Servlet Filter](http://docs.oracle.com/javaee/6/api/javax/servlet/Filter.html) (for logging HTTP requests)
dropwizard-gelf basically starts too late to catch all the startup log messages (e. g. from other bundles or before the
GelfLoggingBundle has been started) and duplicates a lot of functionality from Dropwizard's
[RequestLogHandlerFactory](http://dropwizard.codahale.com/maven/apidocs/com/yammer/dropwizard/config/RequestLogHandlerFactory.html).

On the long run the clean solution would be to add pluggable log handlers to Dropwizard in a way that won't break the
current abstraction.


Acknowledgements
----------------

Thanks to Nick Telford for his [initial version](https://gist.github.com/dd5e000c3327484540a8) of the GraylogBundle.


License
-------

Copyright (c) 2012 smarchive GmbH

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the LICENSE file in this repository for the full license text.


Support
-------

Please log tickets and issues at our [project site](https://github.com/smarchive/dropwizard-gelf/issues).
