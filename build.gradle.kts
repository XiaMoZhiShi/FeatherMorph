import groovy.lang.Closure
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    java
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.5.5"
    id("xyz.jpenilla.run-paper") version "2.0.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0" // Generates plugin.yml
    id("com.github.johnrengelman.shadow") version "7.1.2" // Shadow PluginBase
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }

    maven {
        url = uri("https://repo.md-5.net/content/groups/public/")
    }

    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    maven {
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
/*
    maven {
        url = uri("https://repo.majek.dev/releases")
    }*/
}

dependencies {
    paperweight.paperDevBundle("${project.property("minecraft_version")}")

    compileOnly("com.comphenix.protocol:ProtocolLib:${project.property("protocollib_version")}")

    compileOnly(files("libs/CMILib1.4.3.5.jar"))
    compileOnly(files("libs/Residence5.1.4.0.jar"))
    compileOnly(files("libs/TAB v4.1.2.jar"))

    implementation("org.java-websocket:Java-WebSocket:1.5.6")

    //compileOnly("dev.majek:hexnicks:3.1.1")

    implementation("org.bstats:bstats-bukkit:${project.property("bstats_version")}")
    {
        exclude("com.google.code.gson", "gson")
    }

    val protocolVersion = if (project.property("protocols_use_local_build") == "true")
        project.property("protocols_local_version")
        else project.property("protocols_version");

    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")

    implementation("com.github.XiaMoZhiShi:feathermorph-protocols:${protocolVersion}")
    implementation("com.github.XiaMoZhiShi:PluginBase:${project.property("pluginbase_version")}")
    {
        exclude("com.google.code.gson", "gson")
    }

    //compileOnly("com.github.Gecolay:GSit:${project.property("gsit_version")}")
    compileOnly("me.clip:placeholderapi:${project.property("papi_version")}")
}

group = "xiamomc.morph"
version = "${project.property("project_version")}"
description = "A morph plugin that aims to provide many features out-of-the-box"
java.sourceCompatibility = JavaVersion.VERSION_17

bukkit {
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "xiamomc.morph.MorphPlugin"
    apiVersion = "1.19"
    authors = listOf("MATRIX-feather")
    depend = listOf()
    softDepend = listOf("LibsDisguises", "GSit", "PlaceholderAPI")
    version = "${project.property("project_version")}"
    prefix = "FeatherMorph"
    name = "FeatherMorph"
    foliaSupported = true

    commands {
        register("morph")
        register("morphplayer")
        register("unmorph")

        register("request")

        val featherMorphCommand = register("feathermorph").get()
        featherMorphCommand.aliases = listOf("fm");
    }

    val permissionRoot = "xiamomc.morph."

    permissions {
        register(permissionRoot + "morph")
        register(permissionRoot + "unmorph")
        register(permissionRoot + "headmorph")

        register(permissionRoot + "skill")
        register(permissionRoot + "mirror")
        register(permissionRoot + "chatoverride")

        register(permissionRoot + "request.send")
        register(permissionRoot + "request.accept")
        register(permissionRoot + "request.deny")

        register(permissionRoot + "can_fly")
    }

    permissions.forEach {
        permission -> permission.default = BukkitPluginDescription.Permission.Default.TRUE
    }

    val opPermsStrList = listOf(
            permissionRoot + "disguise_revealing",
            permissionRoot + "manage",
            permissionRoot + "query",
            permissionRoot + "queryall",
            permissionRoot + "reload",
            permissionRoot + "stat",
            permissionRoot + "toggle",

            permissionRoot + "lookup",
            permissionRoot + "skin_cache",
            permissionRoot + "switch_backend",

            permissionRoot + "mirror.immune"
    );

    val opPermsPermList = ArrayList<BukkitPluginDescription.Permission>();

    opPermsStrList.forEach {
        permStr -> permissions.register(permStr).get().default = BukkitPluginDescription.Permission.Default.OP;
    }

    permissions.register(permissionRoot + "can_fly.always").get().default = BukkitPluginDescription.Permission.Default.FALSE;
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        // Workaround for no normal artifact present
        artifact("build/libs/${rootProject.name}-${version}.jar")
    }
}

java {
    withSourcesJar()
}

tasks.build {
    dependsOn(tasks.shadowJar, tasks.reobfJar)
}

tasks.shadowJar {
    minimize()
    relocate("xiamomc.pluginbase", "xiamomc.morph.shaded.pluginbase")
    relocate("org.bstats", "xiamomc.morph.shaded.bstats")
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}
