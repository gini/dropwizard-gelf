Dropwizard GELF
===============
[![Build Status](https://travis-ci.org/gini/dropwizard-gelf.svg?branch=master)](https://travis-ci.org/gini/dropwizard-gelf)
[![Coverage Status](https://img.shields.io/coveralls/gini/dropwizard-gelf.svg)](https://coveralls.io/r/gini/dropwizard-gelf)
[![Maven Central](https://img.shields.io/maven-central/v/net.gini.dropwizard/dropwizard-gelf.svg)](http://mvnrepository.com/artifact/net.gini.dropwizard/dropwizard-gelf)

Addon for Dropwizard adding support for logging to a GELF-enabled server like [Graylog](https://www.graylog.org/)
or [logstash](http://logstash.net/) using [logstash-gelf](http://logging.paluch.biz/).


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
        Thread.currentThread().setUncaughtExceptionHandler(
                UncaughtExceptionHandlers.loggingSystemExitBuilder(NAME, GELF_HOST)
                        .port(GELF_PORT)
                        .build());

        new MyDropwizardApplication().run(args);
    }


Configuration
-------------

The Logback GELF appender can be configured using the provided `GelfConfiguration` class which basically mirrors the
appender configuration outlined in the [logstash-gelf documentation](http://logging.paluch.biz/examples/logback.html).

Your YAML configuration could include the following snippet to configure the `GelfLoggingBundle`:

```
appenders:
  - type: console
  - type: gelf
    host: graylog.example.com
    # port: 12201
    # facility: MyApplication
    # threshold: ALL
    # originHost: my-shiny-host
    extractStackTrace: true
    filterStackTrace: true
    includeFullMDC: true
    additionalFields:
      data_center: DC01
      rack: R5C2
      inception_year: 2016
    additionalFieldTypes:
      inception_year: long
      request_id: long
```


Configuration settings
----------------------

| Setting                | Default                    | Description                                                                                                                                           |
| ---------------------- | -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `enabled`              | `true`                     | Specify if logging to a GELF-compatible server should be enabled.                                                                                     |
| `facility`             | [application name]         | The name of the application. Appears in the `facility` column in the Graylog web interface.                                                           |
| `host`                 | [empty]                    | Hostname/IP-Address of the GELF-compatible server, see [host specification](#host-specification).                                                     |
| `port`                 | `12201`                    | Port of the GELF-compatible server.                                                                                                                   |
| `originHost`           | [FQDN hostname]            | Originating hostname.                                                                                                                                 |
| `extractStackTrace`    | `false`                    | Send the stack-trace to the StackTrace field.                                                                                                         |
| `filterStackTrace`     | `false`                    | Perform stack-trace filtering, see [Stack Trace Filter].                                                                                              |
| `mdcProfiling`         | `false`                    | Perform Profiling (Call-Duration) based on [MDC] Data. See [MDC Profiling] for details.                                                               |
| `additionalFields`     | [empty]                    | Map of additional static fields.                                                                                                                      |
| `additionalFieldTypes` | [empty]                    | Map of type specifications for additional and [MDC] fields. See [Additional field types](#additional-field-types) for details.                        |
| `mdcFields`            | [empty]                    | List of additional fields whose values are obtained from [MDC].                                                                                       |
| `dynamicMdcFields`     | [empty]                    | Dynamic MDC Fields allows you to extract [MDC] values based on one or more regular expressions. The name of the MDC entry is used as GELF field name. |
| `includeFullMdc`       | `false`                    | Include all fields from the [MDC].                                                                                                                    |
| `maximumMessageSize`   | `8192`                     | Maximum message size (in bytes). If the message size is exceeded, the appender will submit the message in multiple chunks (UDP only).                 |
| `timestampPattern`     | `yyyy-MM-dd HH:mm:ss,SSSS` | Date/time pattern for the time field.                                                                                                                 |

[MDC]: http://logback.qos.ch/manual/mdc.html
[MDC Profiling]: http://logging.paluch.biz/mdcprofiling.html
[Stack Trace Filter]: http://logging.paluch.biz/stack-trace-filter.html


### Host specification

* `udp:hostname` for UDP transport, e. g. `udp:127.0.0.1, `udp:some.host.com` or just `some.host.com`.
* `tcp:hostname` for TCP transport, e. g. `tcp:127.0.0.1` or `tcp:some.host.com`. See [TCP transport for logstash-gelf] for details.
* `redis://[:password@]hostname:port/db-number#listname` for Redis transport. See [Redis transport for logstash-gelf] for details.
* `redis-sentinel://[:password@]hostname:port/db-number?masterId=masterId#listname` for Redis transport with Sentinel lookup. See [Redis transport for logstash-gelf] for details.
* `http://host[:port]/[path]` for HTTP transport, e. g. `https://127.0.0.1/gelf`. See [HTTP transport for logstash-gelf] for details.

[TCP transport for logstash-gelf]: http://logging.paluch.biz/tcp.html
[Redis transport for logstash-gelf]: http://logging.paluch.biz/redis.html
[HTTP transport for logstash-gelf]: http://logging.paluch.biz/http.html


### Additional field types

Supported types: `String`, `long`, `Long`, `double`, `Double` and `discover` (default if not specified).


Maven Artifacts
---------------

This project is available on Maven Central. To add it to your project simply add the following dependencies to your POM:

    <dependency>
      <groupId>net.gini.dropwizard</groupId>
      <artifactId>dropwizard-gelf</artifactId>
      <version>0.9.2-1</version>
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
* [Christian Rigdon](https://github.com/Oakie3CR)


License
-------

Copyright (c) 2012-2013 smarchive GmbH, 2013-2016 Gini GmbH, 2015-2016 Jochen Schalanda

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the [LICENSE](LICENSE) file in this repository for the full license text.
