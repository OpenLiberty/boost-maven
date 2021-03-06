# boost-gradle
Liberty Boost Gradle support

# Adding the plugin to a project

Build the plugin locally with:

```
gradle clean install
```

Test the plugin with:

```
gradle clean install check -Druntime=<ol|wlp> -DruntimeVersion=<version> -i -s
```

To use the installed version of the plugin add the following code snippet to your project's `build.gradle` file:

```
buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath 'io.openliberty.boost:boost-gradle-plugin:1.0-M1-SNAPSHOT'
    }
}

apply plugin: 'boost'
```

# Plugin Tasks

| Task Name        | Description                                      |
|------------------|--------------------------------------------------|
| boostStart       | Starts the Boost application.                    |
| boostStop        | Stops the Boost application.                     |
| boostRun         | Runs the Boost application in the foreground.    |
| boostDebug       | Runs the Boost application in debug mode.        |
| boostPackage     | Packages the application and server.             |
| boostDockerBuild | Creates a Docker file and image for the project. |
| boostDockerPush  | Pushes Docker image to a Docker repository.      |

### Spring Boot 1.5 Gradle plugin issues

If you are building a project that uses the Spring Boot 1.5 Gradle plugin you must use a Gradle version lower than Gradle 5.0. The Spring Boot 1.5 Gradle plugin will not be updated to support Gradle 5.0 and up.