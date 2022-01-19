import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

val propsVersion = System.getenv("GRADLE_PROPS_VERSION") ?: "212"
val publishingToken = System.getenv("JB_PUB_TOKEN")

val baseProperties = "base-gradle.properties"
val properties = "gradle-$propsVersion.properties"

val props = Properties()
file(baseProperties).inputStream().let { props.load(it) }
file(properties).inputStream().let { props.load(it) }

fun prop(key: String): String = props[key].toString()

//val intellijVersion = prop("intellijVersion", "2021.2")
val kotlinVersion = "1.6.10"

val pluginJarName = "intellij-move-$propsVersion"
val pluginVersion = "1.4.0"
val pluginGroup = "org.move"

group = pluginGroup
version = pluginVersion

plugins {
    id("java")
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.intellij") version "1.3.1"
    id("org.jetbrains.grammarkit") version "2021.2.1"
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

    val generateRustLexer = task<GenerateLexerTask>("generateMoveLexer") {
        source.set("src/main/grammars/MoveLexer.flex")
        targetDir.set("src/main/gen/org/move/lang")
        targetClass.set("_MoveLexer")
        purgeOldFiles.set(true)
    }

    val generateRustParser = task<GenerateParserTask>("generateMoveParser") {
        source.set("src/main/grammars/MoveParser.bnf")
        targetRoot.set("src/main/gen")
        pathToParser.set("/org/move/lang/MoveParser.java")
        pathToPsiRoot.set("/org/move/lang/psi")
        purgeOldFiles.set(true)
    }

    tasks {
        // workaround for gradle not seeing tests in 2021.3
        val test by getting(Test::class) {
            setScanForTestClasses(false)
            // Only run tests from classes that end with "Test"
            include("**/*Test.class")
        }

        patchPluginXml {
            version.set("$pluginVersion.$propsVersion")
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

        publishPlugin {
            token.set(publishingToken)
        }

        withType<KotlinCompile> {
            dependsOn(generateRustLexer, generateRustParser)
            kotlinOptions {
                jvmTarget = "11"
                languageVersion = "1.6"
                apiVersion = "1.6"
                freeCompilerArgs = listOf("-Xjvm-default=all")
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
