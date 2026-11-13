plugins {
	java
	jacoco
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
	id("info.solidsoft.pitest") version "1.19.0"
}

group = "com.ulearn"
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
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.testcontainers:testcontainers-bom:2.0.5")
	}
}

tasks.named<Jar>("jar") {
	enabled = false
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.named("jacocoTestReport"))
}

jacoco {
	toolVersion = "0.8.14"
}

val coverageExclusions = listOf(
	"com/ulearn/UlearnBackendApplication.class",
)

tasks.named<JacocoReport>("jacocoTestReport") {
	dependsOn(tasks.named("test"))
	reports {
		xml.required = true
		html.required = true
	}
	classDirectories.setFrom(
		files(
			classDirectories.files.map {
				fileTree(it) { exclude(coverageExclusions) }
			},
		),
	)
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
	dependsOn(tasks.named("test"))
	violationRules {
		rule {
			limit {
				counter = "LINE"
				value = "COVEREDRATIO"
				minimum = "0.80".toBigDecimal()
			}
		}
	}
	classDirectories.setFrom(
		files(
			classDirectories.files.map {
				fileTree(it) { exclude(coverageExclusions) }
			},
		),
	)
}

tasks.named("check") {
	dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

// PIT mutation testing. Runs as an explicit `pitest` task (opt-in locally, enforced in the backend
// CI job) rather than being folded into `check`, so the everyday build stays fast. `targetClasses`
// covers the capability packages; generated/Lombok code and the Spring Boot bootstrap are excluded.
// Thresholds are a ratchet, set below the current score, and should be raised as the suite matures
// (issue #14).
pitest {
	pitestVersion = "1.22.1"
	junit5PluginVersion = "1.2.2"
	// The plugin's junit-platform-launcher auto-detection mis-resolves its version under Spring's
	// dependency-management plugin (gradle-pitest-plugin #390); testRuntimeOnly pins it explicitly.
	addJUnitPlatformLauncher = false
	targetClasses = setOf("com.ulearn.*")
	targetTests = setOf("com.ulearn.*")
	excludedClasses = setOf("com.ulearn.UlearnBackendApplication")
	outputFormats = setOf("XML", "HTML")
	timestampedReports = false
	failWhenNoMutations = false
	threads = 2
	mutationThreshold = 70
	coverageThreshold = 80
}
