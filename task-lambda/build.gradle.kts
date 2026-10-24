plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.gmail.alexei28.aws.lambda"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.14.0")
    implementation("software.amazon.awssdk:sqs:2.25.55")

    implementation(platform("software.amazon.awssdk:bom:2.37.0"))
    implementation("software.amazon.awssdk:s3")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Need for AWS Lambda.
// ./gradlew lambdaZip
tasks.register<Zip>("lambdaZip") {
    dependsOn(tasks.classes)

    // For task-lambda, Gradle already knows the project name from: settgings.gradle.kts -> include("task-02")
    archiveFileName.set("${project.name}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    // Application classes at the ZIP root
    from(sourceSets.main.get().output)

    // Runtime dependencies in the lib directory
    into("lib") {
        from(configurations.runtimeClasspath)
    }
}

