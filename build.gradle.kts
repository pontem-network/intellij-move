import org.jetbrains.grammarkit.tasks.GenerateLexer
import org.jetbrains.grammarkit.tasks.GenerateParser

val intellijVersion = prop("intellijVersion", "2021.1.1")
val kotlinVersion = "1.4.32"

val pluginJarName = "intellij-move-$intellijVersion"
val pluginGroup = "org.move"
val pluginVersion = "0.12.0"

group = pluginGroup
version = pluginVersion

plugins {
    id("java")
    id("org.jetbrains.intellij") version "0.7.2"
    id("org.jetbrains.kotlin.jvm") version "1.4.32"
    id("org.jetbrains.grammarkit") version "2021.1.1"
}

dependencies {
    // kotlin stdlib source code
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}

allprojects {
    apply {
        plugin("kotlin")
        plugin("org.jetbrains.grammarkit")
        plugin("org.jetbrains.intellij")
    }

    repositories {
        mavenCentral()
//        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://jetbrains.bintray.com/intellij-third-party-dependencies")
//        maven("http://dl.bintray.com/jetbrains/intellij-plugin-service")
    }

    intellij {
        pluginName = pluginJarName
        version = intellijVersion
    }

    configure<JavaPluginConvention> {
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
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            dependsOn(generateRustLexer, generateRustParser)
            kotlinOptions {
                jvmTarget = "11"
                languageVersion = "1.4"
                apiVersion = "1.4"
                freeCompilerArgs = listOf("-Xjvm-default=compatibility")
            }
        }

        withType<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask> {
            jbrVersion("11_0_9_1b1202.1")
        }

        withType<org.jetbrains.intellij.tasks.RunIdeTask> {
            jbrVersion("11_0_9_1b1202.1")
        }
    }
}

fun hasProp(name: String): Boolean = extra.has(name)

fun prop(name: String, default: String = ""): String {
    val value = extra.properties.getOrDefault(name, default) as String
    if (value.isEmpty()) {
        error("Property `$name` is not defined in gradle.properties")
    }
    return value
}
