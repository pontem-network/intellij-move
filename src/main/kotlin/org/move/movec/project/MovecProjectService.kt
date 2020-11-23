package org.move.movec.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderEx
import java.nio.file.Path

interface MovecProjectService {
}

interface MovecProject : UserDataHolderEx {
    val project: Project
    val manifest: Path
}

val MovecProject.workingDirectory: Path get() = manifest.parent

fun setupMoveProject(project: Project) {}