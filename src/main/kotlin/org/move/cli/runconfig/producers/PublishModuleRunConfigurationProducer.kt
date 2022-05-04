package org.move.cli.runconfig.producers

import com.intellij.psi.PsiElement
import org.move.cli.AptosCommandLine
import org.move.cli.runconfig.AptosCommandLineFromContext
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.isTestOnly
import org.move.lang.moveProject

class PublishModuleRunConfigurationProducer : AptosCommandConfigurationProducer() {

    override fun configFromLocation(location: PsiElement) = fromLocation(location, true)

    companion object {
        fun fromLocation(location: PsiElement, climbUp: Boolean): AptosCommandLineFromContext? {
            val mod = findElement<MvModule>(location, climbUp) ?: return null
            if (mod.isTestOnly) return null

            val rootPath = location.moveProject?.rootPath ?: return null
            val modName = mod.name ?: return null

            val command = "move publish"
            return AptosCommandLineFromContext(
                location,
                "Publish $modName",
                AptosCommandLine(command, rootPath)
            )
        }
    }
}
