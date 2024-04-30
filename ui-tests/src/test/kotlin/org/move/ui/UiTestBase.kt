package org.move.ui

import com.intellij.remoterobot.RemoteRobot
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.move.ui.fixtures.closeProject
import org.move.ui.fixtures.removeLastRecentProject
import org.move.ui.utils.RemoteRobotExtension
import org.move.ui.utils.StepsLogger
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@ExtendWith(RemoteRobotExtension::class)
open class UiTestBase {
    init {
        StepsLogger.init()
    }

    protected fun getResourcesDir(): Path {
        return Paths.get("").toAbsolutePath()
            .resolve("src").resolve("test").resolve("resources")
    }

    @TempDir
    lateinit var tempFolder: File

    protected fun getExamplePackagesDir(): Path = getResourcesDir().resolve("example-packages")
    protected fun copyExamplePackageToTempFolder(packageName: String): Path {
        val tempPackagePath = tempFolder.toPath().resolve(packageName)
        getExamplePackagesDir().resolve(packageName).toFile().copyRecursively(tempPackagePath.toFile())
        Thread.sleep(500)
        return tempPackagePath
    }

    @AfterEach
    fun tearDown(robot: RemoteRobot) = with(robot) {
        closeProject()
        removeLastRecentProject()
    }
}