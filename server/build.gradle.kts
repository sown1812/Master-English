import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

dependencies {
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("io.ktor:ktor-server-status-pages:2.3.12")
    implementation("io.ktor:ktor-server-auth:2.3.12")
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // Config, DB
    implementation("com.typesafe:config:1.4.3")
    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Flyway for migrations
    implementation("org.flywaydb:flyway-core:10.18.2")

    // Firebase auth
    implementation("com.google.firebase:firebase-admin:9.4.2")

    // Tests
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.12")
    testImplementation("com.h2database:h2:2.2.224")
}

application {
    mainClass.set("com.example.server.ApplicationKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

kotlin {
    jvmToolchain(17)
}
