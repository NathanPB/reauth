plugins {
    java
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.10"
}

group = "dev.nathanpb"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-netty:1.5.1")
    implementation("io.ktor:ktor-serialization:1.5.1")
    implementation("org.slf4j:slf4j-simple:1.7.30")
    testImplementation("junit", "junit", "4.12")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "dev.nathanpb.reauth.MainKt"
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = "14"
    }
}
