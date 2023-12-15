plugins {
    id("org.jetbrains.kotlin.jvm") version ("1.8.10")
}

group = "com.oh.plugin.mess"
version = "1.0"

repositories {
    google()
    mavenCentral()

    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
}

dependencies {
    // gradle
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:8.2.0")

    // Guava
    implementation("com.google.guava:guava:31.1-jre")

    // proguard
    implementation("net.sf.proguard:proguard-gradle:6.2.2")
}

tasks.test {
    useJUnitPlatform()
}