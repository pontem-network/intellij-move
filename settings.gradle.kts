rootProject.name = "intellij-move"

pluginManagement {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
}
