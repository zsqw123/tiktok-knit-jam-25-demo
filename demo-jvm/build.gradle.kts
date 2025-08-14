import tiktok.knit.plugin.KnitExtension

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    val knitVersion: String by project
    dependencies {
        classpath("io.github.tiktok.knit:knit-plugin:$knitVersion")
    }
}

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "8.3.6"
    application
}

val knitVersion: String by project
val junitVersion: String by project

apply(plugin = "io.github.tiktok.knit.plugin")

repositories {
    mavenCentral()
}

extensions.getByType<KnitExtension>().apply {
    dependencyTreeOutputPath.set("build/knit.json")
}

dependencies {
    implementation("io.github.tiktok.knit:knit:$knitVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("knit.demo.MainKt")
}
