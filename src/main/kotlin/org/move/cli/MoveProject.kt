package org.move.cli

import com.intellij.openapi.project.Project
import org.move.openapiext.parseToml
import java.nio.file.Path

data class MoveProject(val moveToml: MoveToml) {
    companion object {
        fun fromMoveTomlPath(project: Project, moveTomlPath: Path): MoveProject? {
            val tomlFile = parseToml(project, moveTomlPath) ?: return null
            val moveToml = MoveToml.fromTomlFile(tomlFile) ?: return null
            return MoveProject(moveToml)
        }
    }
}
