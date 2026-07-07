plugins {
    java
    id("com.gradleup.shadow") version "9.4.3"
}

group = "com.azthera"
version = "1.0.0"
description = "EcoCore"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.org/repository/maven-releases/")
    maven("https://jitpack.io")
}

dependencies {

    compileOnly("io.papermc.paper:paper-api:26.2.build.+-alpha")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit")
    }

    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.luckperms:api:5.5")

    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.1")
    implementation("com.mysql:mysql-connector-j:9.1.0")

    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    processResources {

        val props = mapOf(
            "version" to project.version
        )

        inputs.properties(props)

        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {

        archiveClassifier.set("")

        relocate(
            "com.zaxxer.hikari",
            "com.azthera.ecocore.libs.hikari"
        )

        relocate(
            "org.sqlite",
            "com.azthera.ecocore.libs.sqlite"
        )

        relocate(
            "org.mariadb.jdbc",
            "com.azthera.ecocore.libs.mariadb"
        )

        relocate(
            "com.mysql",
            "com.azthera.ecocore.libs.mysql"
        )

        minimize()
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }
}
