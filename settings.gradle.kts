rootProject.name = "intellij-move"

pluginManagement {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        gradlePluginPortal()
    }
}

include("plugin")
include("ui-tests")
