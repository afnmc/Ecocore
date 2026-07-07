plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.azthera"
version = "1.0.0"
description = "EcoCore - Survival Economy Engine for Azthera Network"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()

    // Paper
    maven("https://repo.papermc.io/repository/maven-public/")

    // PlaceholderAPI
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    // CodeMC
    maven("https://repo.codemc.org/repository/maven-public/")

    // OpenCollab
    maven("https://repo.opencollab.dev/main/")

    // JitPack
    maven("https://jitpack.io")
}

dependencies {

    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT")

    // Vault API
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6")

    // LuckPerms
    compileOnly("net.luckperms:api:5.5")

    // Database
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.1")
    implementation("com.mysql:mysql-connector-j:9.1.0")

    // Adventure
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

tasks {

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:all",
                "-parameters"
            )
        )
    }

    processResources {

        val props = mapOf(
            "version" to version
        )

        inputs.properties(props)

        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {

        archiveClassifier.set("")
        archiveBaseName.set("EcoCore")

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

        minimize {

            exclude(dependency("com.zaxxer:HikariCP:.*"))
            exclude(dependency("org.xerial:sqlite-jdbc:.*"))
            exclude(dependency("org.mariadb.jdbc:mariadb-java-client:.*"))
            exclude(dependency("com.mysql:mysql-connector-j:.*"))

        }

    }

    build {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
