# omniture-clickstream
Omniture clickstream parser (alpha version)

Maven configuration:

```xml
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>

<dependency>
  <groupId>com.github.bartekdobija</groupId>
  <artifactId>omniture-clickstream</artifactId>
  <version>71deaff440</version>
</dependency>
```

SBT Configuration:

```sbt

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.bartekdobija" % "omniture-clickstream" % "71deaff440"

```

Gradle configuration:

```gradle
repositories {
    maven {
        url "https://jitpack.io"
    }
}

dependencies {
    compile 'com.github.bartekdobija:omniture-clickstream:71deaff440'
}
```

[![Build Status](https://travis-ci.org/bartekdobija/omniture-clickstream.svg?branch=master)](https://travis-ci.org/bartekdobija/omniture-clickstream)