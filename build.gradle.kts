plugins {
    id("org.jetbrains.kotlin.jvm") version ("1.8.10")
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "com.oh.plugin.android.mess"
version = "1.1"

val ohImplementationClass = "com.oh.plugin.android.mess.MessPlugin"

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

gradlePlugin {
    plugins {
        create("version") {
            id = group as String
            implementationClass = ohImplementationClass
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri(arrayOf(System.getProperty("user.home"), "maven-local", "maven-plugin").joinToString(File.separator))
        }

        maven {
            url = uri(properties["maven_url"] as String)
            credentials {
                username = properties["maven_user_name"] as String
                password = properties["maven_password"] as String
            }
        }
    }

    afterEvaluate {
        publishing {
            publications {
                create<MavenPublication>("product") {
                    from(components["java"])
                    groupId = group as String
                    artifactId = "mess"
                    version = version
                }
            }
        }
    }
}