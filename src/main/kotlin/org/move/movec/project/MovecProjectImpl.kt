package org.move.movec.project

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx

class MovecProjectImpl {
}

private fun setupProjectRoot(project: Project) {
    invokeAndWaitIfNeeded {
        runWriteAction {
            if (project.isDisposed) return@runWriteAction
            ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring {

            }
        }
    }
}