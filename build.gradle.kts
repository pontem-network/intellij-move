import org.jetbrains.grammarkit.tasks.GenerateLexer
import org.jetbrains.grammarkit.tasks.GenerateParser
import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

val propsVersion = System.getenv("GRADLE_PROPS_VERSION") ?: "212"

val baseProperties = "base-gradle.properties"
val properties = "gradle-$propsVersion.properties"

val props = Properties()
file(baseProperties).inputStream().let { props.load(it) }
file(properties).inputStream().let { props.load(it) }

fun prop(key: String): String = props[key].toString()

//val intellijVersion = prop("intellijVersion", "2021.2")
val kotlinVersion = "1.5.31"

val pluginJarName = "intellij-move-$propsVersion"
val pluginVersion = "1.0.0"
val pluginGroup = "org.move"

group = pluginGroup
version = pluginVersion

plugins {
    id("java")
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.intellij") version "1.3.0"
    id("org.jetbrains.grammarkit") version "2021.1.3"
}

dependencies {
    // kotlin stdlib source code
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        main {
            java.srcDirs("src/main/gen")
//            kotlin.srcDirs("src/main/kotlin")
//            resources.srcDirs("src/$platformVersion/main/resources")
        }
//        test {
//            kotlin.srcDirs("src/$platformVersion/test/kotlin")
//            resources.srcDirs("src/$platformVersion/test/resources")
//        }
    }

    val generateRustLexer = task<GenerateLexer>("generateMoveLexer") {
        source = "src/main/grammars/MoveLexer.flex"
        targetDir = "src/main/gen/org/move/lang"
        targetClass = "_MoveLexer"
        purgeOldFiles = true
    }

    val generateRustParser = task<GenerateParser>("generateMoveParser") {
        source = "src/main/grammars/MoveParser.bnf"
        targetRoot = "src/main/gen"
        pathToParser = "/org/move/lang/MoveParser.java"
        pathToPsiRoot = "/org/move/lang/psi"
        purgeOldFiles = true
    }

    tasks {
        patchPluginXml {
            version.set(pluginVersion)
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
                    )
                )
            )
        }

        withType<KotlinCompile> {
            dependsOn(generateRustLexer, generateRustParser)
            kotlinOptions {
                jvmTarget = "11"
                languageVersion = "1.5"
                apiVersion = "1.5"
                freeCompilerArgs = listOf("-Xjvm-default=compatibility")
            }
        }

        withType<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask> {
            jbrVersion.set("11_0_9_1b1202.1")
        }

        withType<org.jetbrains.intellij.tasks.RunIdeTask> {
            jbrVersion.set("11_0_9_1b1202.1")
        }
    }
}

fun prop(name: String, default: String = ""): String {
    val value = extra.properties.getOrDefault(name, default) as String
    if (value.isEmpty()) {
        error("Property `$name` is not defined in gradle.properties")
    }
    return value
}
