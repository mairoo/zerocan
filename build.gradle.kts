plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25" // QueryDSL 지원 추가
	kotlin("plugin.jpa") version "1.9.25" // QueryDSL 지원 추가
	kotlin("kapt") version "1.9.25"
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "kr.pincoin"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

// 버전 상수
object Versions {
	const val KOTLIN_LOGGING_VERSION = "7.0.3"
	const val QUERYDSL_VERSION = "5.1.0"
	const val JJWT_VERSION = "0.12.6"
	const val NETTY_VERSION = "4.1.116.Final"
	const val COMMONS_CODEC_VERSION = "1.17.2"
}

// OS 및 아키텍처 관련 상수
object Platform {
	val nettyClassifier: String? = when {
		System.getProperty("os.name").lowercase().contains("mac") ->
			if (System.getProperty("os.arch").lowercase().contains("aarch64")) "osx-aarch_64"
			else "osx-x86_64"

		else -> null
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
