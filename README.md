# Gradle Metrics Plugin
A plugin to add to your build in order to publish graphite metrics about your gradle tasks. 

## Usage
```
plugins {
    id 'uk.bluedawnsolutions.gradle.metrics-plugin' version '0.1.0'
}

buildMetrics {
    graphiteHost = "enter_your_own_graphite_host_here"
}
```

## Default configuration
Assuming that the machine you are running the build on, has a hostname 
of `dev-machine`, then the default configuration will be:

```
buildMetrics {
    graphiteHost  = "localhost"
    grahpite      = 2003
    metricsPrefix = "gradle.build.dev-machine" 
}
```

Typically, the only configuration you need to supply is a value for the `graphiteHost`

## Building and releasing.
`./gradlew build release`
`./gradlew publishPlugins`
