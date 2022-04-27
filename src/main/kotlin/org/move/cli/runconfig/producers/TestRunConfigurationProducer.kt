package org.move.cli.runconfig.producers

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.move.cli.runconfig.MoveBinaryRunConfigurationProducer
import org.move.cli.runconfig.MoveCmd
import org.move.cli.runconfig.MoveCmdConfig
import org.move.cli.settings.ProjectType
import org.move.cli.settings.type
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.childOfType
import org.move.lang.core.psi.ext.isTest
import org.move.lang.core.psi.ext.isTestOnly
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull

class TestRunConfigurationProducer : MoveBinaryRunConfigurationProducer() {
    override fun configFromLocation(location: PsiElement) = fromLocation(location)

    companion object {
        fun fromLocation(location: PsiElement, climbUp: Boolean = true): MoveCmdConfig? {
            val moveProject = location.moveProject ?: return null
            val packageName = moveProject.packageName ?: return null
            val project = location.project
            val rootPath = moveProject.rootPath ?: return null
            return when (location) {
                is PsiDirectory -> {
                    val locationPath = location.virtualFile.toNioPathOrNull() ?: return null
                    if (locationPath == rootPath
                        || locationPath == moveProject.testsDir()
                    ) {
                        val confName = "Test $packageName"
                        val command = when (project.type) {
                            ProjectType.APTOS -> "move test --package-dir ."
                            ProjectType.DOVE -> "package test"
                        }
                        return MoveCmdConfig(location, confName, MoveCmd(command, rootPath))
                    }
                    null
                }
                is PsiFile -> {
                    val module = location.childOfType<MvModuleDef>() ?: return null
                    fromLocation(module)
                }
                else -> findTestFunction(location, climbUp) ?: findTestModule(location, climbUp)
            }
        }

        private fun findTestFunction(psi: PsiElement, climbUp: Boolean): MoveCmdConfig? {
            val fn = findElement<MvFunction>(psi, climbUp) ?: return null
            if (!fn.isTest) return null
            val functionName = fn.name ?: return null
            val modName = fn.containingModule?.name ?: return null
            val confName = "Test $modName::$functionName"
            val command = when (psi.project.type) {
                ProjectType.DOVE -> "package test --filter $functionName"
                ProjectType.APTOS -> {
                    return null
                }
            }
            val rootPath = fn.moveProject?.rootPath ?: return null
            return MoveCmdConfig(psi, confName, MoveCmd(command, rootPath))
        }

        private fun findTestModule(psi: PsiElement, climbUp: Boolean): MoveCmdConfig? {
            val mod = findElement<MvModuleDef>(psi, climbUp) ?: return null
            if (!mod.isTestOnly) return null
            val modName = mod.name ?: return null
            val confName = "Test $modName"
            val command = when (psi.project.type) {
                ProjectType.DOVE -> "package test --filter $modName"
                ProjectType.APTOS -> {
                    return null
                }
            }
            val rootPath = mod.moveProject?.rootPath ?: return null
            return MoveCmdConfig(psi, confName, MoveCmd(command, rootPath))
        }

        private inline fun <reified T : PsiElement> findElement(base: PsiElement, climbUp: Boolean): T? {
            if (base is T) return base
            if (!climbUp) return null
            return base.parentOfType(withSelf = false)
        }
    }
}
