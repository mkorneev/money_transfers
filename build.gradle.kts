import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.mkorneev"
version = "0.1-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.20"
    id("java")
    id("groovy")
}

val arrow_version = "0.9.0-SNAPSHOT"

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("org.javamoney:moneta:1.3")
    compile("org.iban4j:iban4j:3.2.1")

    compile("io.arrow-kt:arrow-core-data:$arrow_version")
    compile("io.arrow-kt:arrow-core-extensions:$arrow_version")
    compile("io.arrow-kt:arrow-extras-extensions:$arrow_version")

    compile("org.scala-stm:scala-stm_2.10:0.8-SNAPSHOT")

    compile("com.sparkjava:spark-kotlin:1.0.0-alpha")
    compile("org.slf4j:slf4j-simple:1.7.21")

    implementation("com.squareup.moshi:moshi:1.8.0")

    testCompile("org.spockframework:spock-core:1.0-groovy-2.4")
    testCompile("com.despegar:spark-test:1.1.8")

}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}