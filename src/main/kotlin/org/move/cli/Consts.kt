package org.move.cli

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.util.io.exists
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

object Consts {
    const val MANIFEST_FILE = "Move.toml"
    const val ADDR_PLACEHOLDER = "_"

    val PROJECT_SYSTEM_ID = ProjectSystemId("Move")
}
