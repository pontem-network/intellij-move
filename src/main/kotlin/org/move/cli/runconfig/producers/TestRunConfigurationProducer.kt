package org.move.cli.runconfig.producers

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.move.cli.runconfig.MoveBinaryRunConfigurationProducer
import org.move.cli.runconfig.AptosCommandLine
import org.move.cli.runconfig.AptosCommandConf
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.findMoveProject
import org.move.lang.core.psi.ext.isTest
import org.move.lang.core.psi.items

class TestRunConfigurationProducer : MoveBinaryRunConfigurationProducer() {
    override fun configFromLocation(location: PsiElement) = fromLocation(location)

    companion object {
        fun fromLocation(location: PsiElement, climbUp: Boolean = true): AptosCommandConf? {
            return when (location) {
                is PsiFileSystemItem -> {
                    val moveProject = location.findMoveProject() ?: return null
                    val packageName = moveProject.packageName ?: ""
                    val rootPath = moveProject.rootPath ?: return null

                    val confName = "Test $packageName"
                    val command = "move test --package-dir ."
                    return AptosCommandConf(location, confName, AptosCommandLine(command, rootPath))
                }
                else -> findTestFunction(location, climbUp) ?: findTestModule(location, climbUp)
            }
        }

        private fun findTestFunction(psi: PsiElement, climbUp: Boolean): AptosCommandConf? {
            val fn = findElement<MvFunction>(psi, climbUp) ?: return null
            if (!fn.isTest) return null
            val functionName = fn.name ?: return null
            val modName = fn.containingModule?.name ?: return null
            val confName = "Test $modName::$functionName"
            // TODO: add when aptos CLI adds support
            return null
//            val command = when (psi.project.type) {
//                ProjectType.DOVE -> "package test --filter $functionName"
//                ProjectType.APTOS -> {
//                    return null
//                }
//            }
//            val rootPath = fn.moveProject?.rootPath ?: return null
//            return MoveCmdConfig(psi, confName, MoveCmd(command, rootPath))
        }

        private fun findTestModule(psi: PsiElement, climbUp: Boolean): AptosCommandConf? {
            val mod = findElement<MvModule>(psi, climbUp) ?: return null
            if (!hasTestFunction(mod)) return null

            val modName = mod.name ?: return null
            val confName = "Test $modName"
            return null
//            val command = when (psi.project.type) {
//                ProjectType.DOVE -> "package test --filter $modName"
//                ProjectType.APTOS -> {
//                    return null
//                }
//            }
//            val rootPath = mod.moveProject?.rootPath ?: return null
//            return MoveCmdConfig(psi, confName, MoveCmd(command, rootPath))
        }

        private fun hasTestFunction(mod: MvModule): Boolean {
            val items = mod.moduleBlock?.items().orEmpty()
            return items
                .filterIsInstance<MvFunction>()
                .any { it.isTest }
        }
    }
}
