package org.move.cli.externalSystem

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.move.cli.Consts
import org.move.cli.MoveProject
import org.move.cli.moveProjectsService
import org.move.stdext.blankToNull

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
            out.add("$root/${Consts.MANIFEST_FILE}")

            val packageName = movePackage.packageName.blankToNull()
            if (packageName != null) {
                out.add("$root/build/$packageName/BuildInfo.yaml")
            }
        }
    }

    companion object {
        fun getInstance(project: Project): MoveSettingsFilesService = project.service()
    }
}
