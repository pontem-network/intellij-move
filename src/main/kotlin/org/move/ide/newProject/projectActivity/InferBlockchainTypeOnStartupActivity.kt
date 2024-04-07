package org.move.ide.newProject.projectActivity

import com.intellij.ide.util.RunOnceUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.readText
import org.move.cli.settings.Blockchain.APTOS
import org.move.cli.settings.Blockchain.SUI
import org.move.cli.settings.moveSettings
import org.move.openapiext.rootDir

class InferBlockchainTypeOnStartupActivity: ProjectActivity {
    override suspend fun execute(project: Project) {
        RunOnceUtil.runOnceForProject(project, ID) {
            val moveToml = project.rootDir?.findChild("Move.toml") ?: return@runOnceForProject
            val moveTomlText = moveToml.readText()
            val blockchain =
                when {
                    moveTomlText.contains("https://github.com/MystenLabs/sui.git") -> SUI
                    else -> APTOS
                }

            if (project.moveSettings.blockchain != blockchain) {
                project.moveSettings.modify {
                    it.blockchain = blockchain
                }
            }
        }
    }

    companion object {
        const val ID = "org.move.InferBlockchainTypeOnStartupActivity"
    }
}