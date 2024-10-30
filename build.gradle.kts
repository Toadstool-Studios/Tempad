import groovy.json.StringEscapeUtils
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    idea
    kotlin("jvm") version "2.0.20"
    id("maven-publish")
    id("com.teamresourceful.resourcefulgradle") version "0.0.+"
    id("net.neoforged.gradle.userdev") version "7.0.153"
}

val minecraftVersion: String by project
val modId: String by project

base {
    archivesName.set("$modId-$minecraftVersion")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)
jarJar.enable()

repositories {
    maven(url = "https://maven.architectury.dev/")
    maven(url = "https://maven.neoforged.net/releases")
    maven(url = "https://maven.teamresourceful.com/repository/maven-public/")
    maven(url = "https://maven.twelveiterations.com/repository/maven-public/")
    maven(url = "https://maven.octo-studios.com/releases")
    maven(url = "https://modmaven.dev/" )
    maven(url = "https://api.modrinth.com/maven")
    maven(url = "https://maven.blamejared.com" )
    maven(url = "https://cursemaven.com" )
    mavenLocal()
}

dependencies {
    val neoforgeVersion: String by project
    val minecraftVersion: String by project

    val resourcefulConfigVersion: String by project
    val resourcefulLibVersion: String by project
    val resourcefulLibKtVersion: String by project
    val kotlinForForgeVersion: String by project
    val curiosVersion: String by project
    val mekanismVersion: String by project
    val patchouliVersion: String by project
    val jadeVersion: String by project
    val jeiVersion: String by project
    val arsNouveauVersion: String by project

    implementation("net.neoforged:neoforge:${neoforgeVersion}")

    compileOnly("mekanism:Mekanism:${mekanismVersion}:api")

    runtimeOnly("mekanism:Mekanism:${mekanismVersion}")
    runtimeOnly("mekanism:Mekanism:${mekanismVersion}:additions")
    runtimeOnly("mekanism:Mekanism:${mekanismVersion}:generators")
    runtimeOnly("mekanism:Mekanism:${mekanismVersion}:tools")

    implementation("com.teamresourceful.resourcefulconfig:resourcefulconfig-neoforge-${minecraftVersion}:${resourcefulConfigVersion}")
    implementation("com.teamresourceful.resourcefullib:resourcefullib-neoforge-${minecraftVersion}:${resourcefulLibVersion}")
    compileOnly("com.teamresourceful:bytecodecs:1.1.0")
    implementation("thedarkcolour:kotlinforforge-neoforge:${kotlinForForgeVersion}")
    implementation("com.teamresourceful.resourcefullibkt:resourcefullibkt-neoforge-${minecraftVersion}:${resourcefulLibKtVersion}") {
        isTransitive = false
    }

    jarJar(group = "com.teamresourceful.resourcefullibkt", name = "resourcefullibkt-neoforge-${minecraftVersion}", version = resourcefulLibKtVersion).also {
        jarJar.pin(it, "[${resourcefulLibKtVersion})")
    }

    implementation(group = "earth.terrarium.olympus", name = "olympus-neoforge-${minecraftVersion}", version = "1.0.10") {
        isTransitive = false
    }.also { jarJar(it) }

    implementation(group = "earth.terrarium.argonauts", name = "argonauts-neoforge-${minecraftVersion}", version = "2.0.0") {
        isTransitive = false
    }

    implementation(group = "earth.terrarium.cadmus", name = "cadmus-neoforge-${minecraftVersion}", version = "2.0.0") {
        isTransitive = false
    }

    implementation("top.theillusivec4.curios:curios-neoforge:${curiosVersion}")

    compileOnly("vazkii.patchouli:Patchouli:${patchouliVersion}:api")
    runtimeOnly("vazkii.patchouli:Patchouli:${patchouliVersion}")

    implementation("maven.modrinth:jade:$jadeVersion")

    // compile against the JEI API but do not include it at runtime
    compileOnly("mezz.jei:jei-${minecraftVersion}-neoforge-api:${jeiVersion}")
    // at runtime, use the full JEI jar for NeoForge
    runtimeOnly("mezz.jei:jei-${minecraftVersion}-neoforge:${jeiVersion}")

    implementation("com.hollingsworth.ars_nouveau:ars_nouveau-${minecraftVersion}.0:${arsNouveauVersion}") {
        exclude(group = "curse.maven")
    }

    compileOnly(group = "curse.maven", name = "ftb-teams-forge-404468", version = "5631446")

    implementation(group = "earth.terrarium.common_storage_lib", name = "common-storage-lib-data-neoforge-$minecraftVersion", version = "0.0.1") {
        isTransitive = false
    }.let { jarJar(it) }
}

java {
    withSourcesJar()
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    filesMatching(listOf("META-INF/neoforge.mods.toml")) {
        expand("version" to project.version)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjvm-default=all")
        freeCompilerArgs.add("-Xcontext-receivers")
    }

    sourceSets.all {
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "$modId-$minecraftVersion"
            from(components["java"])

            pom {
                name.set("Tempad")
                url.set("https://github.com/terrarium-earth/$modId")

                scm {
                    connection.set("git:https://github.com/terrarium-earth/$modId.git")
                    developerConnection.set("git:https://github.com/terrarium-earth/$modId.git")
                    url.set("https://github.com/terrarium-earth/$modId")
                }

                licenses {
                    license {
                        name.set("ARR")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            setUrl("https://maven.resourcefulbees.com/repository/terrarium/")
            credentials {
                username = System.getenv("MAVEN_USER")
                password = System.getenv("MAVEN_PASS")
            }
        }
    }
}

resourcefulGradle {
    templates {
        register("embed") {
            val minecraftVersion: String by project
            val version: String by project
            val changelog: String = file("changelog.md").readText(Charsets.UTF_8)
            val fabricLink: String? = System.getenv("FABRIC_RELEASE_URL")
            val forgeLink: String? = System.getenv("FORGE_RELEASE_URL")

            source.set(file("templates/embed.json.template"))
            injectedValues.set(mapOf(
                    "minecraft" to minecraftVersion,
                    "version" to version,
                    "changelog" to StringEscapeUtils.escapeJava(changelog),
                    "fabric_link" to fabricLink,
                    "forge_link" to forgeLink
            ))
        }
    }
}

runs {
    // other run configurations here

    maybeCreate("data").apply {
        programArguments.addAll(
            "--mod", modId,
            "--all",
            "--output", file("src/generated/resources").absolutePath,
            "--existing", file("src/main/resources/").absolutePath,
            "--client",
            "--server"
        )
    }
}

sourceSets.main.configure {
    resources {
        srcDir("src/generated/resources")
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}