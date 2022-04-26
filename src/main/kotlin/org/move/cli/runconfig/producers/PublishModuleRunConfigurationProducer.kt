package org.move.cli.runconfig.producers

import com.intellij.psi.PsiElement
import org.move.cli.runconfig.MoveBinaryRunConfigurationProducer
import org.move.cli.runconfig.MoveCmdConf
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.lang.core.psi.ext.isTestOnly
import org.move.lang.moveProject
import org.move.cli.settings.ProjectType
import org.move.cli.settings.type
import org.move.cli.settings.moveSettings

class PublishModuleRunConfigurationProducer: MoveBinaryRunConfigurationProducer() {
    override fun configFromLocation(location: PsiElement): MoveCmdConf? {
        val moveProject = location.moveProject ?: return null
        val rootPath = moveProject.rootPath ?: return null

        val mod = location.ancestorOrSelf<MvModuleDef>() ?: return null
        if (mod.isTestOnly) return null
        val modName = mod.name ?: return null

        val privateKey = location.project.moveSettings.settingsState.privateKey
        val command = when (location.project.type) {
            ProjectType.APTOS -> "move publish --package-dir . --private-key $privateKey"
            ProjectType.DOVE -> {
                "deploy"
            }
        }
        return MoveCmdConf("Publish module $modName", command, rootPath)
    }
}
