import groovy.json.StringEscapeUtils

plugins {
    kotlin("jvm") version "2.0.0"
    id("earth.terrarium.cloche") version "0.1.5"

    id("com.teamresourceful.resourcefulgradle") version "0.0.+"

    `maven-publish`
}

val modId: String by project

val parchmentMinecraftVersion: String by project
val parchmentMappingsVersion: String by project

val minecraftVersion: String by project
val neoforgeVersion: String by project

private val _minecraftVersion = minecraftVersion

repositories {
    maven(url = "https://maven.architectury.dev/")
    maven(url = "https://maven.neoforged.net/releases")
    maven(url = "https://maven.teamresourceful.com/repository/maven-public/")
    maven(url = "https://maven.twelveiterations.com/repository/maven-public/")
    maven(url = "https://maven.octo-studios.com/releases")
    maven(url = "https://modmaven.dev/" )
    maven(url = "https://api.modrinth.com/maven")
    mavenLocal()
}

tasks.jar {
    archiveClassifier.set(minecraftVersion)
}

cloche {
    val _modId = modId

    metadata {
        modId.set(_modId)
    }

    minecraftVersion.set(_minecraftVersion)

    neoforge {
        loaderVersion.set(neoforgeVersion)
    }

    mappings {
        official()
        parchment(parchmentMappingsVersion, minecraftVersion=parchmentMinecraftVersion)
    }
}

dependencies {
    val resourcefulConfigVersion: String by project
    val resourcefulLibVersion: String by project
    val resourcefulLibKtVersion: String by project
    val kotlinForForgeVersion: String by project
    val curiosVersion: String by project
    val mekanismVersion: String by project
    val patchouliVersion: String by project
    val jadeVersion: String by project
    val jeiVersion: String by project

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

    /*
    jarJar(group = "com.teamresourceful.resourcefullibkt", name = "resourcefullibkt-neoforge-${minecraft_version}", version = resourcefulLibKtVersion).also {
        jarJar.pin(it, "[${resourcefulLibKtVersion})")
    }
    */

    implementation(group = "earth.terrarium.olympus", name = "olympus-neoforge-$minecraftVersion", version = "1.0.1") {
        isTransitive = false
    }

    /*
    jarJar(group = "earth.terrarium.olympus", name = "olympus-neoforge-$minecraftVersion", version = "1.0.1").also {
        jarJar.pin(it, "[1.0.1)")
    }
    */

    implementation("top.theillusivec4.curios:curios-neoforge:${curiosVersion}")

    compileOnly("vazkii.patchouli:Patchouli:${patchouliVersion}:api")
    runtimeOnly("vazkii.patchouli:Patchouli:${patchouliVersion}")

    implementation("maven.modrinth:jade:$jadeVersion")

    // compile against the JEI API but do not include it at runtime
    compileOnly("mezz.jei:jei-${minecraftVersion}-common-api:${jeiVersion}")
    compileOnly("mezz.jei:jei-${minecraftVersion}-neoforge-api:${jeiVersion}")
    // at runtime, use the full JEI jar for NeoForge
    runtimeOnly("mezz.jei:jei-${minecraftVersion}-neoforge:${jeiVersion}")
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
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
            val changelog: String = file("changelog.md").readText(Charsets.UTF_8)
            val forgeLink: String? = System.getenv("FORGE_RELEASE_URL")

            source.set(file("templates/embed.json.template"))
            injectedValues.set(mapOf(
                "minecraft" to minecraftVersion,
                "version" to version.toString(),
                "changelog" to StringEscapeUtils.escapeJava(changelog),
                "forge_link" to forgeLink
            ))
        }
    }
}
