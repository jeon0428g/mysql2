plugins {
    id("org.springframework.boot") version "3.1.0"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    kotlin("jvm") version "1.8.0" // Kotlin 플러그인 추가
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web") // 추가된 부분
    implementation("mysql:mysql-connector-java:8.0.26") // 필요에 따라 버전 변경
    implementation("org.springframework.boot:spring-boot-starter") // Spring Boot Starter
    testImplementation("org.springframework.boot:spring-boot-starter-test") // Test Starter
    // JPA API
    implementation("javax.persistence:javax.persistence-api:2.2") // 필요에 따라 버전 변경

    implementation("org.hibernate.orm:hibernate-core:6.0.0.Final") // Hibernate 6.x 사용
    implementation("jakarta.persistence:jakarta.persistence-api:3.0.0") // JPA API

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

//    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("javax.ws.rs:javax.ws.rs-api:2.1")

}

tasks.withType<Test> {
    useJUnitPlatform()
}