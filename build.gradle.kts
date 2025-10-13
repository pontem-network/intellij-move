import org.jetbrains.intellij.platform.gradle.Constants.Constraints
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

@Suppress("USELESS_ELVIS_RIGHT_IS_NULL")
val publishingToken = System.getenv("JB_PUB_TOKEN") ?: null
val isCI = System.getenv("CI") != null
val isLocal = !isCI && System.getenv("MKURNIKOV_LOCAL") != null

fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined in gradle.properties for environment `$shortPlatformVersion`")

val shortPlatformVersion = prop("shortPlatformVersion")
val useInstaller = prop("useInstaller").toBooleanStrict()
val codeVersion = "1.47.0"

val pluginVersion = "$codeVersion.$shortPlatformVersion"
val pluginGroup = "org.move"
val pluginName = "intellij-move"

val kotlinReflectVersion = "2.2.0"

group = pluginGroup
version = pluginVersion

plugins {
    id("java")
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.9.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
    id("net.saliman.properties") version "1.5.2"
}

allprojects {
    apply {
        plugin("kotlin")
        plugin("org.jetbrains.grammarkit")
        plugin("org.jetbrains.intellij.platform")
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
        intellijPlatform {
            defaultRepositories()
            jetbrainsRuntime()
        }
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinReflectVersion")

        implementation("io.sentry:sentry:7.14.0") {
            exclude("org.slf4j")
        }
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.+")
        // required for the StringEscapeUtils
        implementation("org.apache.commons:commons-text:1.12.0")

        testImplementation("junit:junit:4.13.2")
        testImplementation("org.opentest4j:opentest4j:1.3.0")

        intellijPlatform {
            if (isLocal) {
                local("/snap/rustrover/current")
            } else {
                create(prop("platformType"), prop("platformVersion")) {
                    this.useInstaller = useInstaller
                }
            }

            pluginVerifier(Constraints.LATEST_VERSION)
            bundledPlugin("org.toml.lang")
            jetbrainsRuntime()

            testFramework(TestFrameworkType.Platform)
        }
    }

    sourceSets {
        main {
            java.srcDirs("src/main/gen")
        }
    }

    grammarKit {
        grammarKitRelease.set("2023.3")
    }

    kotlin {
        jvmToolchain(21)
        if (file("src/$shortPlatformVersion/main/kotlin").exists()) {
            sourceSets {
                main {
                    kotlin.srcDirs("src/$shortPlatformVersion/main/kotlin")
                }
            }
        }
    }

    intellijPlatform {
        pluginConfiguration {
            version.set(pluginVersion)
            ideaVersion {
                sinceBuild.set(shortPlatformVersion)
                untilBuild.set("$shortPlatformVersion.*")
            }

//            val codeVersionForUrl = codeVersion.replace('.', '-')
            changeNotes.set(
                """
    <body>
        <p><a href="https://github.com/pontem-network/intellij-move/releases/tag/v$codeVersion">
            Changelog for the Intellij-Move $codeVersion
            </a></p>
    </body>
            """
            )
        }

        instrumentCode.set(false)

        publishing {
            token.set(publishingToken)
        }

        pluginVerification {
            ides {
                if (isCI) {
                    recommended()
                }
            }
            failureLevel.set(
                EnumSet.complementOf(
                    EnumSet.of(
                        // these are the only issues we tolerate
                        VerifyPluginTask.FailureLevel.DEPRECATED_API_USAGES,
                        VerifyPluginTask.FailureLevel.EXPERIMENTAL_API_USAGES,
                        VerifyPluginTask.FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES,
                    )
                )
            )
        }
    }

    tasks {
        withType<Test> {
            minHeapSize = "1024m"
            maxHeapSize = "4096m"
        }
        compileKotlin {
            compilerOptions {
                languageVersion.set(KotlinVersion.KOTLIN_2_1)
                apiVersion.set(KotlinVersion.KOTLIN_2_1)
                freeCompilerArgs.add("-Xjvm-default=all")
            }
        }

        jar {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        generateLexer {
            sourceFile.set(file("src/main/grammars/MoveLexer.flex"))
            targetOutputDir.set(file("src/main/gen/org/move/lang"))
            purgeOldFiles.set(true)
        }
        generateParser {
            sourceFile.set(file("src/main/grammars/MoveParser.bnf"))
            targetRootOutputDir.set(file("src/main/gen"))
            // not used if purgeOldFiles set to false
            pathToParser.set("/org/move/lang/MoveParser.java")
            pathToPsiRoot.set("/org/move/lang/core/psi")
            purgeOldFiles.set(true)
        }
        withType<KotlinCompile> {
            dependsOn(generateLexer, generateParser)
        }
    }

    @Suppress("unused")
    val runIdeWithPlugins by intellijPlatformTesting.runIde.registering {
        plugins {
            plugin("com.google.ide-perf:1.3.2")
//            plugin("PsiViewer:PsiViewer 241.14494.158-EAP-SNAPSHOT")
        }
        task {
            systemProperty("org.move.debug.enabled", true)
            systemProperty("org.move.types.highlight.unknown.as.error", false)
            systemProperty("intellij.idea.indices.debug", true)
            systemProperty("intellij.idea.indices.debug.extra.sanity", true)
//            systemProperty("org.move.external.linter.max.duration", 30)  // 30 ms
//            systemProperty("org.move.aptos.bundled.force.unsupported", true)
//            systemProperty("idea.log.debug.categories", "org.move.cli")
        }
    }
}

