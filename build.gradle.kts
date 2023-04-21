plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.5.0"
}

repositories {
    mavenLocal()
    maven("https://repo.dmulloy2.net/repository/public/")
    maven ("https://repo.maven.apache.org/maven2/")
}

dependencies {
    api("org.jetbrains:annotations:23.0.0")
    paperweight.paperDevBundle("1.19.3-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")
}

group = "dev.trxsson"
version = "1.2.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

publishing {
    publications.create<MavenPublication>("mavenJava") {
        from(components["java"])
        artifact("${project.buildDir.toPath()}/libs/${project.name}-${version}.jar")
    }
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
}