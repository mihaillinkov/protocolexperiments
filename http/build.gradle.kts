plugins {
    id("buildlogic.kotlin-common-conventions")
    application
}

application {
    // Define the main class for the application.
    mainClass = "http.MainKt"
}

tasks.named<JavaExec>("run") {
    // This line copies all properties set on the command line (-D)
    // to the application's forked JVM.
    systemProperties = System.getProperties().mapKeys { "${it.key}" }.toMap()
}

dependencies {
    // https://mvnrepository.com/artifact/com.influxdb/influxdb-client-kotlin
    implementation("com.influxdb:influxdb-client-kotlin:7.3.0")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

}