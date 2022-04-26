package org.move.cli.runconfig.producers

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.cli.runconfig.MoveBinaryRunConfigurationProducer
import org.move.cli.runconfig.MoveCmdConf
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.ancestors
import org.move.lang.core.psi.ext.childOfType
import org.move.lang.core.psi.ext.isTest
import org.move.lang.core.psi.ext.isTestOnly
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull
import org.move.cli.settings.ProjectType
import org.move.cli.settings.type

class TestRunConfigurationProducer : MoveBinaryRunConfigurationProducer() {
    override fun configFromLocation(location: PsiElement): MoveCmdConf? {
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
                    return MoveCmdConf(confName, command, rootPath)
                }
                null
            }
            is PsiFile -> {
                val module = location.childOfType<MvModuleDef>() ?: return null
                configFromLocation(module)
            }
            else -> {
                for (ans in location.ancestors) {
                    when (ans) {
                        is MvFunction -> {
                            if (!ans.isTest) continue
                            val functionName = ans.name ?: return null
                            val modName = ans.containingModule?.name ?: return null
                            val confName = "Test $packageName: $modName::$functionName"
                            val command = when (project.type) {
                                ProjectType.DOVE -> "package test --filter ${functionName}"
                                ProjectType.APTOS -> {
                                    return null
                                }
                            }
                            return MoveCmdConf(confName, command, rootPath)
                        }
                        is MvModuleDef -> {
                            if (!ans.isTestOnly) continue
                            val modName = ans.name ?: return null
                            val confName = "Test $packageName: $modName"
                            val command = when (project.type) {
                                ProjectType.DOVE -> "package test --filter $modName"
                                ProjectType.APTOS -> {
                                    return null
                                }
                            }
                            return MoveCmdConf(confName, command, rootPath)
                        }
                        else -> continue
                    }
                }
                null
            }
        }
    }
}
