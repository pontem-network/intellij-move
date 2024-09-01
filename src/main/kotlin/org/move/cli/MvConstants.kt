package org.move.cli

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import org.move.stdext.exists
import java.nio.file.Path

object MvProjectLayout {
    val sourcesDirs = arrayOf("sources", "examples", "scripts")
    const val testsDir = "tests"
    const val buildDir = "build"

    fun dirs(root: Path): List<Path> {
        val names = listOf(*sourcesDirs, testsDir, buildDir)
        return names.map { root.resolve(it) }.filter { it.exists() }
    }
}

object MvConstants {
    const val MANIFEST_FILE = "Move.toml"
    const val ADDR_PLACEHOLDER = "_"

    const val PSI_FACTORY_DUMMY_FILE = "DUMMY_PSI_FACTORY.move"

    val PROJECT_SYSTEM_ID = ProjectSystemId("Move")
}
