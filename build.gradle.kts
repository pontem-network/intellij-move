import org.jetbrains.intellij.platform.gradle.Constants.Constraints
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.util.*

val publishingToken = System.getenv("JB_PUB_TOKEN") ?: null
val publishingChannel = System.getenv("JB_PUB_CHANNEL") ?: "default"

// set by default in Github Actions
val isCI = System.getenv("CI") != null

fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined in gradle.properties for environment `$shortPlatformVersion`")

fun propOrNull(name: String): String? = extra.properties[name] as? String

fun gitCommitHash(): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = "git rev-parse --short HEAD".split(" ")
//            commandLine = "git rev-parse --abbrev-ref HEAD".split(" ")
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim().also {
        if (it == "HEAD")
            logger.warn("Unable to determine current branch: Project is checked out with detached head!")
    }
}

fun gitTimestamp(): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = "git show --no-patch --format=%at HEAD".split(" ")
//            commandLine = "git rev-parse --abbrev-ref HEAD".split(" ")
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim().also {
        if (it == "HEAD")
            logger.warn("Unable to determine current branch: Project is checked out with detached head!")
    }
}

val shortPlatformVersion = prop("shortPlatformVersion")
val useInstaller = prop("useInstaller").toBooleanStrict()
val codeVersion = "1.38.0"

var pluginVersion = "$codeVersion.$shortPlatformVersion"
if (publishingChannel != "default") {
    // timestamp of the commit with this eaps addition
    val start = 1714498465
    val commitTimestamp = gitTimestamp().toInt() - start
    pluginVersion = "$pluginVersion-$publishingChannel.$commitTimestamp"
}

val pluginGroup = "org.move"
val pluginName = "intellij-move"
val pluginJarName = "intellij-move-$pluginVersion"

val javaVersion = JavaVersion.VERSION_17
//val javaVersion = if (shortPlatformVersion == "241") JavaVersion.VERSION_17 else JavaVersion.VERSION_21
val kotlinReflectVersion = "1.9.10"
val aptosVersion = "3.4.1"

val remoteRobotVersion = "0.11.22"

group = pluginGroup
version = pluginVersion

plugins {
    id("java")
    kotlin("jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.0.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
    id("net.saliman.properties") version "1.5.2"
    id("org.gradle.idea")
    id("de.undercouch.download") version "5.5.0"
}

allprojects {
    apply {
        plugin("kotlin")
        plugin("org.jetbrains.grammarkit")
        plugin("org.jetbrains.intellij.platform")
        plugin("de.undercouch.download")
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

        implementation("io.sentry:sentry:7.2.0") {
            exclude("org.slf4j")
        }
        implementation("com.github.ajalt.clikt:clikt:3.5.2")
        testImplementation("junit:junit:4.13.2")

        intellijPlatform {
            create(prop("platformType"), prop("platformVersion"), useInstaller = useInstaller)
            testFramework(TestFrameworkType.Platform)
            pluginVerifier(Constraints.LATEST_VERSION)
            bundledPlugin("org.toml.lang")
            jetbrainsRuntime("17.0.11b1207.30")
        }
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
                sinceBuild.set(prop("pluginSinceBuild"))
                untilBuild.set(prop("pluginUntilBuild"))
            }

            val codeVersionForUrl = codeVersion.replace('.', '-')
            changeNotes.set(
                """
    <body>
        <p><a href="https://intellij-move.github.io/$codeVersionForUrl.html">
            Changelog for the Intellij-Move $codeVersion
            </a></p>
    </body>
            """
            )
        }

        instrumentCode.set(false)

        publishing {
            token.set(publishingToken)
            channels.set(listOf(publishingChannel))
        }

        pluginVerification {
            ides {
                recommended()
            }
//            if ("SNAPSHOT" !in shortPlatformVersion) {
//                ides {
//                    ide(prop("verifierIdeVersion").trim())
//                }
//            }
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
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "17"
                languageVersion = "1.9"
                apiVersion = "1.9"
                freeCompilerArgs = listOf("-Xjvm-default=all")
            }
        }

        withType<Jar> {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        task("downloadAptosBinaries") {
            val baseUrl = "https://github.com/aptos-labs/aptos-core/releases/download/aptos-cli-v$aptosVersion"
            doLast {
                // NOTE: MacOS is not supported anymore for pre-built CLI
                for (releasePlatform in listOf(/*"MacOSX",*/ "Ubuntu-22.04", "Ubuntu", "Windows")) {
                    val zipFileName = "aptos-cli-$aptosVersion-$releasePlatform-x86_64.zip"
                    val zipFileUrl = "$baseUrl/$zipFileName"
                    val buildRoot = rootProject.layout.buildDirectory.get()
                    val zipRoot = "$buildRoot/zip"
                    val zipFile = file("$zipRoot/$zipFileName")
                    if (!zipFile.exists()) {
                        download.run {
                            src(zipFileUrl)
                            dest(zipFile)
                            overwrite(false)
                        }
                    }

                    val platformName =
                        when (releasePlatform) {
//                            "MacOSX" -> "macos"
                            "Ubuntu" -> "ubuntu"
                            "Ubuntu-22.04" -> "ubuntu22"
                            "Windows" -> "windows"
                            else -> error("unreachable")
                        }
                    val platformRoot = file("${rootProject.rootDir}/bin/$platformName")
                    copy {
                        from(
                            zipTree(zipFile)
                        )
                        into(platformRoot)
                    }
                }
            }
        }

        generateLexer {
            sourceFile.set(file("src/main/grammars/MoveLexer.flex"))
            targetOutputDir.set(file("src/main/gen/org/move/lang"))
            purgeOldFiles.set(true)
        }
        generateParser {
            sourceFile.set(file("src/main/grammars/MoveParser.bnf"))
            targetRootOutputDir.set(file("src/main/gen"))
            pathToParser.set("/org/move/lang/MoveParser.java")
            pathToPsiRoot.set("/org/move/lang/psi")
            purgeOldFiles.set(true)
        }
        withType<KotlinCompile> {
            dependsOn(generateLexer, generateParser)
        }

        runIde {
            systemProperty("org.move.debug.enabled", true)
//            systemProperty("org.move.external.linter.max.duration", 30)  // 30 ms
//            systemProperty("org.move.aptos.bundled.force.unsupported", true)
//            systemProperty("idea.log.debug.categories", "org.move.cli")
        }

        prepareSandbox {
//            enabled = true
            dependsOn("downloadAptosBinaries")
            // copy bin/ directory inside the plugin zip file
            from("$rootDir/bin") {
                into("$pluginName/bin")
                include("**")
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

    idea {
        pathVariables(mapOf("USER_HOME" to file("/home/mkurnikov")))
        module {
            name = "intellij-move.main"
        }
    }
}

//project(":ui-tests") {
//    dependencies {
//        implementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
//        implementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")
//        implementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
//
//        implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
//
//        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
//        testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
//
//        implementation("com.automation-remarks:video-recorder-junit5:2.0")
//    }
//
//    tasks.named<Test>("test") {
//        useJUnitPlatform()
//    }
//
//    tasks {
//        ideaModule {
//            enabled = false
//        }
//    }

//        downloadRobotServerPlugin {
//            version.set(remoteRobotVersion)
//        }

//        runIdeForUiTests {
//            systemProperty("robot-server.port", "8082")
////            systemProperty "ide.mac.message.dialogs.as.sheets", "false"
////            systemProperty "jb.privacy.policy.text", "<!--999.999-->"
////            systemProperty "jb.consents.confirmation.enabled", "false"
////            systemProperty "ide.mac.file.chooser.native", "false"
////            systemProperty "jbScreenMenuBar.enabled", "false"
////            systemProperty "apple.laf.useScreenMenuBar", "false"
//            systemProperty("idea.trust.all.projects", "true")
//            systemProperty("ide.show.tips.on.startup.default.value", "false")
//        }

//}

val Project.dependencyCachePath
    get(): String {
        val cachePath = file("${rootProject.projectDir}/deps")
        // If cache path doesn't exist, we need to create it manually
        // because otherwise gradle-intellij-plugin will ignore it
        if (!cachePath.exists()) {
            cachePath.mkdirs()
        }
        return cachePath.absolutePath
    }

