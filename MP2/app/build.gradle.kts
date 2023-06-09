/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("mp2.java-application-conventions")
}

dependencies {
    implementation("org.apache.logging.log4j:log4j-api:2.19.0")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("com.google.guava:guava:31.1-jre")

    implementation(files("../libs/disruptor-3.4.4.jar"))
    implementation("commons-io:commons-io:+");
    implementation("org.apache.commons:commons-text")
    implementation("com.google.code.gson:gson:2.9.1")
}

application {
    // Define the main class for the application.
    mainClass.set("mp3.app.App")
}

tasks {
    named<JavaExec>("run") {
        systemProperty("log4j2.contextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")
        standardInput = System.`in`
    }
}