package org.move.cli.runConfigurations.producers

import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.aptos.AptosCommandLine
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.isTest
import org.move.lang.core.psi.ext.isTestOnly
import org.move.lang.moveProject

class PublishCommandConfigurationProducer : AptosCommandConfigurationProducer() {

    override fun configFromLocation(location: PsiElement) = fromLocation(location, true)

    companion object {
        fun fromLocation(location: PsiElement, climbUp: Boolean): AptosCommandLineFromContext? {
            // no publish if inside test function
            val fn = findElement<MvFunction>(location, climbUp)
            if (fn != null && fn.isTest) return null

            val mod = findElement<MvModule>(location, climbUp) ?: return null
            if (mod.isTestOnly) return null

            val rootPath = location.moveProject?.contentRootPath ?: return null
            val modName = mod.name ?: return null
            return AptosCommandLineFromContext(
                location,
                "Publish $modName",
                AptosCommandLine("move publish", workingDirectory = rootPath)
            )
        }
    }
}
