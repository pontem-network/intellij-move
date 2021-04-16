package org.move.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.Paths

val Project.rootDir: VirtualFile?
    get() = this.basePath?.let { VirtualFileManager.getInstance().findFileByNioPath(Paths.get(it)) }