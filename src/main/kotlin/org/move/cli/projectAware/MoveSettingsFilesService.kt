package org.move.cli.projectAware

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.move.cli.Consts
import org.move.cli.moveProjects

@Service
class MoveSettingsFilesService(private val project: Project) {
    fun collectSettingsFiles(): List<String> {
        val out = mutableListOf<String>()
        for (moveProject in project.moveProjects.allProjects) {
            for (movePackage in moveProject.movePackages()) {
                val root = movePackage.contentRoot.path
                out.add("$root/${Consts.MANIFEST_FILE}")
            }
        }
        return out
    }

    companion object {
        fun getInstance(project: Project): MoveSettingsFilesService = project.service()
    }
}
