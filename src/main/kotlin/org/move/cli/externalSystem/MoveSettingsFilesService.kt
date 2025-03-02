package org.move.cli.externalSystem

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.move.cli.MoveProject
import org.move.cli.MvConstants
import org.move.cli.moveProjectsService

@Service(Service.Level.PROJECT)
class MoveSettingsFilesService(private val project: Project) {

    fun collectSettingsFiles(): Set<String> {
        val out = mutableSetOf<String>()
        for (moveProject in project.moveProjectsService.allProjects) {
            moveProject.collectSettingsFiles(out)
        }
        return out
    }

    private fun MoveProject.collectSettingsFiles(out: MutableSet<String>) {
        for (movePackage in this.movePackages()) {
            val root = movePackage.contentRoot.path
            out.add("$root/${MvConstants.MANIFEST_FILE}")
        }
    }

    companion object {
        fun getInstance(project: Project): MoveSettingsFilesService = project.service()
    }
}
