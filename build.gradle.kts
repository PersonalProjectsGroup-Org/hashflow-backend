plugins {
    java
    id("org.springframework.boot") version "3.2.5" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "com.hashflow"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}