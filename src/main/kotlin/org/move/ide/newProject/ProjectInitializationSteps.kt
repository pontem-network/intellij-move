package org.move.ide.newProject

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.MvConstants
import org.move.openapiext.contentRoots

fun Project.guessMoveTomlFile(): VirtualFile? {
    val packageRoot = this.contentRoots.firstOrNull()
    if (packageRoot != null) {
        val manifest = packageRoot.findChild(MvConstants.MANIFEST_FILE)
        return manifest
    }
    return null
}

