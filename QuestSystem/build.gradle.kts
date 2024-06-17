plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "2.3.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.schotzgoblin"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://repo.codemc.io/repository/maven-snapshots/") {
        name = "codemc-snapshots"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
    implementation("com.github.TheSilentPro:HeadDB:5.0.0-rc.11")
    implementation("net.wesjd:anvilgui:1.9.4-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    runServer {
        minecraftVersion("1.20.4")
    }

    test {
        useJUnitPlatform()
    }

    named("build") {
        dependsOn("shadowJar")
        finalizedBy("copyJarToBin")
    }

    register<Copy>("copyJarToBin") {
        copy{
            from("build/libs/QuestSystem-1.0-all.jar")
            into("C:\\Users\\Max\\Documents\\GitHub\\QuestSystemPlugin\\lobby-server\\plugins")
        }
        copy{
            from("build/libs/QuestSystem-1.0-all.jar")
            into("C:\\Users\\Max\\Documents\\GitHub\\QuestSystemPlugin\\lobby-server2\\plugins")
        }
    }
}
