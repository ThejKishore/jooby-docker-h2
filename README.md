# myapp-java

Welcome to myapp-java!!

## running

    ./gradlew joobyRun

## building

    ./gradlew build

## docker

     docker build . -t myapp-java
     docker run -p 8080:8080 -it myapp-java

## Logging in Jooby (how it is set up here)

- Jooby uses SLF4J for logging. In this project the backend is Logback via the dependency `io.jooby:jooby-logback` (see build.gradle).
- Logback auto-discovers its configuration from the classpath. The active config file in this project is at `src/main/resources/conf/logback.xml`.
- We keep two files to avoid confusion:
  - `src/main/resources/conf/logback.xml` = active (packaged on the classpath)
  - `conf/logback.xml` = intentionally inert (left empty to prevent accidental use)

### Current defaults
- Root logger: ERROR (keeps the app quiet)
- Jetty: WARN (suppresses noisy DEBUG from the web server)
- Jooby framework: INFO (so you can see startup summary and framework messages)
- Access logs: handled by `io.jooby.handler.AccessLogHandler` and routed to a dedicated `ACCESS` appender.

To change levels, edit `src/main/resources/conf/logback.xml` or override at runtime:

- Via JVM system property (example: enable full Jooby INFO and app DEBUG):

  ```bash
  ./gradlew joobyRun -Dlogger.level.io.jooby=INFO -Dlogger.level.app=DEBUG
  ```

- Or run the fat jar with overrides:

  ```bash
  java -Dlogger.level.io.jooby=INFO -Dlogger.level.org.eclipse.jetty=WARN -jar build/libs/myapp-java-*-all.jar
  ```

You can also create environment-specific configs by activating different classpath files (e.g., keep multiple logback*.xml variants and select with `-Dlogback.configurationFile=conf/logback-dev.xml`).
