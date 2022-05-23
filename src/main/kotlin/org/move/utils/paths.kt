package org.move.utils

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.Path
import java.nio.file.Paths

interface ProjectRootService {
    val path: Path?

    val pathFile: VirtualFile?
        get() = path?.let { VirtualFileManager.getInstance().findFileByNioPath(it) }
}

class ProjectRootServiceImpl(private val project: Project) : ProjectRootService {
    override val path: Path?
        get() = project.basePath?.let { Paths.get(it) }
}

class TestProjectRootServiceImpl(private val project: Project) : ProjectRootService {
    private var _path: Path = Paths.get("")

    override val path: Path
        get() = _path

    fun modifyPath(path: Path) {
        _path = path
    }
}

val Project.rootService: ProjectRootService get() = service()
