plugins {
    java
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.10"
}

group = "dev.nathanpb"
version = "0.4.4"

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
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-kotlinx-serialization:2.3.1")
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.2.4")
    implementation("com.github.ben-manes.caffeine:caffeine:3.0.0")
    implementation("com.auth0:java-jwt:3.14.0")
    implementation("org.graalvm.js:js:21.0.0.2")
    implementation("com.apurebase:kgraphql:0.17.2")
    implementation("com.apurebase:kgraphql-ktor:0.17.2")
    implementation("io.projectreactor:reactor-core:3.4.4")
    implementation("commons-codec:commons-codec:1.15")

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
        jvmTarget = "11"
    }
}
