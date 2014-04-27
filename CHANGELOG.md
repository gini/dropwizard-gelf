Dropwizard GELF Changelog
=========================

0.6.0
-----

* Finalize migration to Dropwizard 0.7.x (thanks again to [Michal Svab](https://github.com/msvab))
* Use application name as default for 'facility' configuration setting.


0.5.0
-----

* Upgrade to Java 7
* Upgrade to Dropwizard 0.7.0 (thanks to [Michal Svab](https://github.com/msvab))
* Properly clean up MDC in GelfLoggingFilter instead of purging the complete contents
* GelfLoggingFilter should also clean up and log if chain.doFilter() throws an exception


0.4.2
-----

* Properly clean up MDC in GelfLoggingFilter instead of purging the complete contents
* GelfLoggingFilter should also clean up and log if chain.doFilter() throws an exception


0.4.1
-----

* Fix regression: Make configuration setting 'hostName' optional


0.4.0
-----

* Add configuration setting 'hostname' to override the hostname in GELF messages
* Upgrade to logback-gelf 0.10p2


0.3.0
-----

* Rebranding from smarchive to Gini (package names, Maven coordinates)
* Switch to [semantic versioning](http://semver.org/)
* Add GelfBootstrap and UncaughtExceptionHandlers
* Improve GelfLoggingFilter
* Add configuration setting shortMessagePattern
* Add configuration setting useMarker
* Add support for static additional fields
* Add configuration setting includeFullMDC
* Upgrade to logback-gelf 0.10p1


0.2
---

* Upgrade to Dropwizard 0.6.2
* Fix race condition in GelfLoggingFilter$CountingHttpServletResponseWrapper


0.1
---

* Initial release
