# Failed Attempt to Improve Development Cycle Time

David Mohs, August 2017

## Problem

Development cycle time is the time from when a change is made to a source code file until the time that change is reflected in the running process and can be tested by the developer. The most straightforward path is to rebuild and restart the application from scratch whenever the developer changes a file.

On my machine (MBP mid-2014), a compile and start cycle takes approximately one minute. I would rate this cycle time as extremely poor.

## Other Data

The directory structure accepted by Google's Development AppEngine Server is the WAR format, but "exploded" into a full structure instead of wrapped in a single file. In our case, the Swagger-generated class files live alongside the compiled source files under the WEB-INF/classes directory.

Google's development server will automatically restart if either WEB-INF/appengine-web.xml or WEB-INF/web.xml is touched.

## Failed Experiments

### Failed Experiment 1: Rebuild WAR and Restart

Running `./gradlew assemble` will build the code and assemble the result into the requisite WAR directory structure. In our project, this also touches appengine-web.xml, so the server automatically restarts.

This approach has two drawbacks. First, the XML file is touched part-way through the process, so there is a potential race condition where the server begins the restart process before assembly completes.

Second, the assembly itself takes half a minute. Combine that with the half a minute required for the restart itself and we've lost any time savings.

### Failed Experiment 2: Compile and Copy

Running `./gradlew compileJava` will rebuild the changed Java source files in just a few seconds. We can then reload the server by (1) copying the files into the exploded WAR directory and (2) touching appengine-web.xml to restart the server.

Automating this would require some scripting, partially because of the intermingling of the source classes and Swagger-generated classes in the exploded WAR directory. However, the benefit is still minimal. Since the startup time of the application is quite long (half a minute), the benefit may not be worth the cost of scripting this process.

## Future Work

Spring Boot's documentation claims to support automatic restarts when files change on the classpath [1]. When I added `spring-boot-devtools`, I got the following stack trace:

```
java.lang.IllegalArgumentException: Unable to find the main class to restart
```

I attempted a few things to fix this but gave up.

[1] https://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-devtools.html#using-boot-devtools-restart
