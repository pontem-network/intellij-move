import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

val platformVersion = prop("shortPlatformVersion")
val publishingToken = System.getenv("JB_PUB_TOKEN") ?: null

fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined in gradle.properties for environment `$platformVersion`")

val pluginJarName = "intellij-move-$platformVersion"
val pluginVersion = "1.30.1"
val pluginGroup = "org.move"
val javaVersion = JavaVersion.VERSION_17
val kotlinStdlibVersion = "1.9.0"

group = pluginGroup
version = pluginVersion

plugins {
    idea
    id("java")
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.15.0"
    id("org.jetbrains.grammarkit") version "2022.3.1"
    id("net.saliman.properties") version "1.5.2"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinStdlibVersion")

    implementation("io.sentry:sentry:6.25.0") {
        exclude("org.slf4j")
    }
    implementation("com.github.ajalt.clikt:clikt:3.5.2")
}

project(":") {
    apply {
        plugin("kotlin")
        plugin("org.jetbrains.grammarkit")
        plugin("org.jetbrains.intellij")
    }

    repositories {
        mavenCentral()
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        gradlePluginPortal()
    }

    intellij {
        pluginName.set(pluginJarName)
        version.set(prop("platformVersion"))
        type.set(prop("platformType"))

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
        plugins.set(prop("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    sourceSets {
        main {
            java.srcDirs("src/main/gen")
        }
    }

    kotlin {
        sourceSets {
            main {
                kotlin.srcDirs("src/$platformVersion/main/kotlin")
            }
        }
    }

    val generateMoveLexer = task<GenerateLexerTask>("generateMoveLexer") {
        sourceFile.set(file("src/main/grammars/MoveLexer.flex"))
        targetDir.set("src/main/gen/org/move/lang")
        targetClass.set("_MoveLexer")
        purgeOldFiles.set(true)
    }

    val generateMoveParser = task<GenerateParserTask>("generateMoveParser") {
        sourceFile.set(file("src/main/grammars/MoveParser.bnf"))
        targetRoot.set("src/main/gen")
        pathToParser.set("/org/move/lang/MoveParser.java")
        pathToPsiRoot.set("/org/move/lang/psi")
        purgeOldFiles.set(true)
    }

    tasks {
        patchPluginXml {
            version.set("$pluginVersion.$platformVersion")
            changeNotes.set("""
    <body>
        <p><a href="https://github.com/pontem-network/intellij-move/blob/master/changelog/$pluginVersion.md">
            Changelog for Intellij-Move $pluginVersion on Github
            </a></p>
    </body>
            """)
            sinceBuild.set(prop("pluginSinceBuild"))
            untilBuild.set(prop("pluginUntilBuild"))
        }

        runPluginVerifier {
            ideVersions.set(
                prop("verifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty)
            )
            failureLevel.set(
                EnumSet.complementOf(
                    EnumSet.of(
                        // these are the only issues we tolerate
                        RunPluginVerifierTask.FailureLevel.DEPRECATED_API_USAGES,
                        RunPluginVerifierTask.FailureLevel.EXPERIMENTAL_API_USAGES,
                        RunPluginVerifierTask.FailureLevel.INTERNAL_API_USAGES,
                        RunPluginVerifierTask.FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES,
                    )
                )
            )
        }

        publishPlugin {
            token.set(publishingToken)
        }

        withType<KotlinCompile> {
            dependsOn(
                generateMoveLexer, generateMoveParser
            )
            kotlinOptions {
                jvmTarget = "17"
                languageVersion = "1.8"
                apiVersion = "1.6"
                freeCompilerArgs = listOf("-Xjvm-default=all")
            }
        }

        withType<Jar> {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        withType<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask> {
            jbrVersion.set(prop("jbrVersion"))
        }

        withType<org.jetbrains.intellij.tasks.RunIdeTask> {
            jbrVersion.set(prop("jbrVersion"))

            if (environment.getOrDefault("CLION_LOCAL", "false") == "true") {
                val clionDir = File("/snap/clion/current")
                if (clionDir.exists()) {
                    ideDir.set(clionDir)
                }
            }
        }
    }

    task("resolveDependencies") {
        doLast {
            rootProject.allprojects
                .map { it.configurations }
                .flatMap { it.filter { c -> c.isCanBeResolved } }
                .forEach { it.resolve() }
        }
    }
}
