package org.move.cli.runconfig.producers

import com.intellij.psi.PsiElement
import org.move.cli.runconfig.MoveBinaryRunConfigurationProducer
import org.move.cli.runconfig.MoveCmdConf
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.lang.core.psi.ext.isTestOnly
import org.move.lang.moveProject
import org.move.settings.ProjectKind
import org.move.settings.kind

class PublishModuleRunConfigurationProducer: MoveBinaryRunConfigurationProducer() {
    override fun configFromLocation(location: PsiElement): MoveCmdConf? {
        val moveProject = location.moveProject ?: return null
        val rootPath = moveProject.rootPath ?: return null

        val mod = location.ancestorOrSelf<MvModuleDef>() ?: return null
        if (mod.isTestOnly) return null
        val modName = mod.name ?: return null

        val command = when (location.project.kind) {
            ProjectKind.APTOS -> "move publish --package-dir ."
            ProjectKind.DOVE -> {
                "deploy"
            }
        }
        return MoveCmdConf("Publish module $modName", command, rootPath)
    }
}
