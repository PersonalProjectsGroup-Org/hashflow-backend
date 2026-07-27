plugins {
    java
    id("org.springframework.boot") version "3.3.0" apply false
}

allprojects {
    group = "com.hashflow"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}