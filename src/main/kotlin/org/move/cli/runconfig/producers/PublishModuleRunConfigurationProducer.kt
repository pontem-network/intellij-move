package org.move.cli.runconfig.producers

import com.intellij.psi.PsiElement
import org.move.cli.runconfig.MoveBinaryRunConfigurationProducer
import org.move.cli.runconfig.AptosCommandLine
import org.move.cli.runconfig.AptosCommandConf
import org.move.cli.settings.moveSettings
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.isTestOnly
import org.move.lang.moveProject

class PublishModuleRunConfigurationProducer : MoveBinaryRunConfigurationProducer() {
    override fun configFromLocation(location: PsiElement): AptosCommandConf? {
        val mod = findElement<MvModule>(location, true) ?: return null
        if (mod.isTestOnly) return null

        val rootPath = location.moveProject?.rootPath ?: return null
        val modName = mod.name ?: return null

        val privateKey = location.project.moveSettings.settingsState.privateKey
        val command = "move publish --package-dir . --private-key $privateKey"
        return AptosCommandConf(
            location,
            "Publish $modName",
            AptosCommandLine(command, rootPath)
        )
    }
}
