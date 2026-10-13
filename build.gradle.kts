// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
plugins {
    // Kotlin follows the version Spring Boot's BOM manages (2.3.21 for Boot 4.1.1); bumped together with Boot.
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.jpa") version "2.3.21"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.ef_softworks"
// The image tag is the git tag; CI passes it in as -PappVersion so the jar and /info report the same version.
version = providers.gradleProperty("appVersion").getOrElse("0.0.0-dev")
description = "Pathfinder server: device-aware QR redirects and the management API of the console"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Console login: BCrypt password hashes; the bearer filter and the authorization rules are Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")

    // HS256 access and refresh tokens of the console
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // QR image rendering (PNG straight from the library, SVG built from the bit matrix)
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")

    // Platform and device brand from the User-Agent (iOS / Huawei / Android / everything else). The library logs
    // through log4j-api, which Spring Boot's logging starter bridges to SLF4J.
    implementation("nl.basjes.parse.useragent:yauaa:8.2.0")

    runtimeOnly("org.postgresql:postgresql")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

springBoot {
    // META-INF/build-info.properties -> BuildProperties bean -> GET /api/v1/info carries the version
    buildInfo()
}

tasks.bootJar {
    // Stable file name: the Dockerfile copies it without knowing the version
    archiveFileName = "pathfinder-server.jar"
}

tasks.withType<Test> {
    useJUnitPlatform()
}
