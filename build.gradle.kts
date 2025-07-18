group = "io.hexlet"
version = "0.0.1-SNAPSHOT"
description = "Hexlet Typo Reporter"
java.sourceCompatibility = JavaVersion.VERSION_21

plugins {
    id("java")
    id("maven-publish")
    id("io.freefair.lombok") version "8.6"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.springframework.boot") version "3.0.4"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("checkstyle")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:1.17.6")
    }
}

dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation ("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.session:spring-session-core")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    // Thymeleaf
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6:3.1.1.RELEASE")
    implementation("io.github.jpenren:thymeleaf-spring-data-dialect:3.6.0")
    implementation("org.webjars:webjars-locator-core:0.58")
    implementation("org.webjars:bootstrap:5.2.3")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.3.0")
    // Database
    runtimeOnly("org.postgresql:postgresql:42.5.5")
    implementation("io.hypersistence:hypersistence-utils-hibernate-60:3.2.0")
    implementation("org.liquibase:liquibase-core:4.26.0")
    // Utils
    compileOnly("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    implementation("org.ocpsoft.prettytime:prettytime:5.0.6.Final")
    implementation("org.mapstruct:mapstruct:1.5.3.Final")
    implementation("io.github.cdimascio:dotenv-java:3.2.0")
    implementation("org.antlr:antlr4-runtime:4.10.1")
    // Annotation processors
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.3.Final")
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.github.database-rider:rider-spring:1.36.0")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock:4.0.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:1.5.3.Final")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Amapstruct.defaultComponentModel=spring")
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.create("unitTest", type = Test::class) {
    filter {
        includeTestsMatching("${project.group}.${rootProject.name}.domain.*")
        includeTestsMatching("${project.group}.${rootProject.name}.repository.*")
        includeTestsMatching("${project.group}.${rootProject.name}.service.*")
    }
}

tasks.create("integrationTest", type = Test::class) {
    filter {
        includeTestsMatching("${project.group}.${rootProject.name}.web.*")
    }
}


tasks {
    register("stage") {
        dependsOn("bootJar", "clean")
    }

    named("bootJar") {
        mustRunAfter("clean")
    }
}
