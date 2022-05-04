package org.move.cli.runconfig.producers

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.move.cli.AptosCommandLine
import org.move.cli.runconfig.AptosCommandLineFromContext
import org.move.lang.MvFile
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.hasTestFunctions
import org.move.lang.core.psi.ext.findMoveProject
import org.move.lang.core.psi.ext.isTest
import org.move.lang.modules
import org.move.lang.moveProject

class TestRunConfigurationProducer : AptosCommandConfigurationProducer() {

    override fun configFromLocation(location: PsiElement) = fromLocation(location)

    companion object {
        fun fromLocation(location: PsiElement, climbUp: Boolean = true): AptosCommandLineFromContext? {
            return when (location) {
                is MvFile -> {
                   val module = location.modules().firstOrNull() ?: return null
                   findTestModule(module, climbUp)
                }
                is PsiFileSystemItem -> {
                    val moveProject = location.findMoveProject() ?: return null
                    val packageName = moveProject.packageName ?: ""
                    val rootPath = moveProject.rootPath ?: return null

                    val confName = "Test $packageName"
                    val command = "move test"
                    AptosCommandLineFromContext(location, confName, AptosCommandLine(command, rootPath))
                }
                else -> findTestFunction(location, climbUp) ?: findTestModule(location, climbUp)
            }
        }

        private fun findTestFunction(psi: PsiElement, climbUp: Boolean): AptosCommandLineFromContext? {
            val fn = findElement<MvFunction>(psi, climbUp) ?: return null
            if (!fn.isTest) return null
            val functionName = fn.name ?: return null
            val modName = fn.containingModule?.name ?: return null
            val confName = "Test $modName::$functionName"
            val command = "move test --filter $functionName"
            val rootPath = fn.moveProject?.rootPath ?: return null
            return AptosCommandLineFromContext(fn, confName, AptosCommandLine(command, rootPath))
        }

        private fun findTestModule(psi: PsiElement, climbUp: Boolean): AptosCommandLineFromContext? {
            val mod = findElement<MvModule>(psi, climbUp) ?: return null
            if (!mod.hasTestFunctions()) return null

            val modName = mod.name ?: return null
            val confName = "Test $modName"
            val command = "move test --filter $modName"
            val rootPath = mod.moveProject?.rootPath ?: return null
            return AptosCommandLineFromContext(mod, confName, AptosCommandLine(command, rootPath))
        }
    }
}
