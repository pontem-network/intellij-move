package org.move.cli.runconfig.producers

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.move.cli.AptosCommandLine
import org.move.cli.MoveProject
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.findMoveProject
import org.move.lang.core.psi.ext.hasTestFunctions
import org.move.lang.core.psi.ext.isTest
import org.move.lang.modules
import org.move.lang.moveProject
import org.toml.lang.psi.TomlFile

class TestCommandConfigurationProducer : AptosCommandConfigurationProducer() {

    override fun configFromLocation(location: PsiElement) = fromLocation(location)

    companion object {
        fun fromLocation(location: PsiElement, climbUp: Boolean = true): AptosCommandLineFromContext? {
            return when {
                location is MoveFile -> {
                    val module = location.modules().firstOrNull { it.hasTestFunctions() } ?: return null
                    findTestModule(module, climbUp)
                }
                location is TomlFile && location.name == "Move.toml" -> {
                    val moveProject = location.findMoveProject() ?: return null
                    findTestProject(location, moveProject)
                }
                location is PsiDirectory -> {
                    val moveProject = location.findMoveProject() ?: return null
                    if (
                        location.virtualFile == moveProject.currentPackage.contentRoot
                        || location.virtualFile == moveProject.currentPackage.testsFolder
                    ) {
                        findTestProject(location, moveProject)
                    } else {
                        null
                    }
                }
                else -> findTestFunction(location, climbUp) ?: findTestModule(location, climbUp)
            }
        }

        private fun findTestFunction(psi: PsiElement, climbUp: Boolean): AptosCommandLineFromContext? {
            val fn = findElement<MvFunction>(psi, climbUp) ?: return null
            if (!fn.isTest) return null

            val modName = fn.containingModule?.name ?: return null
            val functionName = fn.name ?: return null

            val confName = "Test $modName::$functionName"
            val command = "move test --filter $modName::$functionName"
            val rootPath = fn.moveProject?.contentRootPath ?: return null
            return AptosCommandLineFromContext(fn, confName, AptosCommandLine(command, rootPath))
        }

        private fun findTestModule(psi: PsiElement, climbUp: Boolean): AptosCommandLineFromContext? {
            val mod = findElement<MvModule>(psi, climbUp) ?: return null
            if (!mod.hasTestFunctions()) return null

            val modName = mod.name ?: return null
            val confName = "Test $modName"
            val command = "move test --filter $modName"
            val rootPath = mod.moveProject?.contentRootPath ?: return null
            return AptosCommandLineFromContext(mod, confName, AptosCommandLine(command, rootPath))
        }

        private fun findTestProject(
            location: PsiFileSystemItem,
            moveProject: MoveProject
        ): AptosCommandLineFromContext? {
            val packageName = moveProject.currentPackage.packageName
            val rootPath = moveProject.contentRootPath ?: return null

            val confName = "Test $packageName"
            val command = "move test"
            return AptosCommandLineFromContext(location, confName, AptosCommandLine(command, rootPath))
        }
    }
}
