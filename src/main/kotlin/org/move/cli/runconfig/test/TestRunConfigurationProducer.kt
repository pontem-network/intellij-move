package org.move.cli.runconfig.test

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.cli.runconfig.MoveBinaryRunConfigurationProducer
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.ancestors
import org.move.lang.core.psi.ext.childOfType
import org.move.lang.core.psi.ext.isTest
import org.move.lang.core.psi.ext.isTestOnly
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull

class TestRunConfigurationProducer : MoveBinaryRunConfigurationProducer() {
    override fun configFromLocation(location: PsiElement): TestConfig? {
        val moveProject = location.moveProject ?: return null
        val packageName = moveProject.packageName ?: return null

        return when (location) {
            is PsiDirectory -> {
                val locationPath = location.virtualFile.toNioPathOrNull() ?: return null
                val rootPath = moveProject.rootPath ?: return null
                if (locationPath == rootPath
                    || locationPath == moveProject.testsDir()
                ) {
                    return TestConfig.Package(packageName, rootPath)
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
                            val path = moveProject.root.toNioPathOrNull() ?: return null
                            return TestConfig.Function(packageName, functionName, modName, path)
                        }
                        is MvModuleDef -> {
                            if (!ans.isTestOnly) continue
                            val modName = ans.name ?: return null
                            val path = moveProject.root.toNioPathOrNull() ?: return null
                            return TestConfig.Module(packageName, modName, path)
                        }
                        else -> continue
                    }
                }
                null
            }
        }
    }
}
