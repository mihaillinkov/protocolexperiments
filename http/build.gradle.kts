plugins {
    id("buildlogic.kotlin-common-conventions")
    application
}

application {
    // Define the main class for the application.
    mainClass = "http.MainKt"
}

dependencies {
    // https://mvnrepository.com/artifact/com.influxdb/influxdb-client-kotlin
    implementation("com.influxdb:influxdb-client-kotlin:7.3.0")
}