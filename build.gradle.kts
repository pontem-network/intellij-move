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



//val intellijVersion = prop("intellijVersion", "2021.2")
val kotlinVersion = "1.8.10"

val pluginJarName = "intellij-move-$platformVersion"
val pluginVersion = "1.24.0"
val pluginGroup = "org.move"
val javaVersion = if (platformVersion < "222") JavaVersion.VERSION_11 else JavaVersion.VERSION_17
val kotlinJvmTarget = if (platformVersion < "222") "11" else "17"

group = pluginGroup
version = pluginVersion

plugins {
    id("java")
    kotlin("jvm") version "1.8.10"
    id("org.jetbrains.intellij") version "1.9.0"
    id("org.jetbrains.grammarkit") version "2021.2.2"
    id("net.saliman.properties") version "1.5.2"
}

dependencies {
    // kotlin stdlib source code
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
}

allprojects {
    apply {
        plugin("kotlin")
        plugin("org.jetbrains.grammarkit")
        plugin("org.jetbrains.intellij")
    }

    repositories {
        mavenCentral()
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
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
//            test {
//                kotlin.srcDirs("src/$platformVersion/test/kotlin")
//            }
        }
    }

    val generateMoveLexer = task<GenerateLexerTask>("generateMoveLexer") {
        source.set("src/main/grammars/MoveLexer.flex")
        targetDir.set("src/main/gen/org/move/lang")
        targetClass.set("_MoveLexer")
        purgeOldFiles.set(true)
    }

    val generateMoveParser = task<GenerateParserTask>("generateMoveParser") {
        source.set("src/main/grammars/MoveParser.bnf")
        targetRoot.set("src/main/gen")
        pathToParser.set("/org/move/lang/MoveParser.java")
        pathToPsiRoot.set("/org/move/lang/psi")
        purgeOldFiles.set(true)
    }

    tasks {
        // workaround for gradle not seeing tests in 2021.3+
        val test by getting(Test::class) {
            setScanForTestClasses(false)
            // Only run tests from classes that end with "Test"
            include("**/*Test.class")
        }

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
                jvmTarget = kotlinJvmTarget
                languageVersion = "1.8"
                apiVersion = "1.5"
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
            ideDir.set(File("/snap/clion/current"))
        }
    }
}
