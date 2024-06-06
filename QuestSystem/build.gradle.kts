plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

group = "com.schotzgoblin"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

configurations {}

dependencies {
    implementation("org.hibernate:hibernate-core:6.1.2.Final")
    implementation ("com.h2database:h2:2.2.220")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
    implementation("org.hibernate:hibernate-jpamodelgen:6.1.1.Final")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.0")
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
    jar {
        val directory = file(projectDir).parent
        destinationDirectory.set(file("$directory\\lobby-server2\\plugins"))
    }

    jar {
        val directory = file(projectDir).parent
        destinationDirectory.set(file("$directory\\lobby-server\\plugins"))
    }
}
