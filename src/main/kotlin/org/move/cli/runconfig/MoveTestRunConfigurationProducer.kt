package org.move.cli.runconfig

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.ancestors
import org.move.lang.core.psi.ext.childOfType
import org.move.lang.core.psi.ext.isTest
import org.move.lang.core.psi.ext.isTestOnly
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull
import java.nio.file.Path

class MoveTestRunConfigurationProducer : MoveRunConfigurationProducer() {

    override fun setupConfigurationFromContext(
        templateConfiguration: MoveRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val testConfig = TestConfig.fromLocation(sourceElement.get()) ?: return false
        templateConfiguration.name = testConfig.testName()
        templateConfiguration.cmd = testConfig.commandLine()
        return true
    }

    override fun isConfigurationFromContext(
        configuration: MoveRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val location = context.psiLocation ?: return false
        val testConfig = TestConfig.fromLocation(location) ?: return false
        return configuration.name == testConfig.testName()
                && configuration.cmd == testConfig.commandLine()
    }
}


sealed interface TestConfig {
    fun testName(): String
    fun commandLine(): MoveCommandLine

    data class Function(
        val packageName: String,
        val functionName: String,
        val modName: String,
        val path: Path
    ) : TestConfig {
        override fun testName() = "Test $packageName: $modName::$functionName"
        override fun commandLine() = MoveCommandLine(
            "package test --filter ${this.functionName}",
            this.path
        )
    }

    data class Package(val packageName: String, val path: Path) : TestConfig {
        override fun testName() = "Test $packageName"
        override fun commandLine() = MoveCommandLine("package test", this.path)
    }

    data class Module(val packageName: String, val modName: String, val path: Path) : TestConfig {
        override fun testName() = "Test $packageName: $modName"
        override fun commandLine() = MoveCommandLine("package test --filter $modName", this.path)
    }

    companion object {
        fun fromLocation(location: PsiElement): TestConfig? {
            val moveProject = location.moveProject ?: return null
            val packageName = moveProject.packageName ?: return null

            return when (location) {
                is PsiDirectory -> {
                    val locationPath = location.virtualFile.toNioPathOrNull() ?: return null
                    val rootPath = moveProject.rootPath ?: return null
                    if (locationPath == rootPath
                        || locationPath == moveProject.testsDir()
                    ) {
                        return Package(packageName, rootPath)
                    }
                    null
                }
                is PsiFile -> {
                    val module = location.childOfType<MvModuleDef>() ?: return null
                    fromLocation(module)
                }
                else -> {
                    for (ans in location.ancestors) {
                        when (ans) {
                            is MvFunction -> {
                                if (!ans.isTest) continue
                                val functionName = ans.name ?: return null
                                val modName = ans.containingModule?.name ?: return null
                                val path = moveProject.root.toNioPathOrNull() ?: return null
                                return Function(packageName, functionName, modName, path)
                            }
                            is MvModuleDef -> {
                                if (!ans.isTestOnly) continue
                                val modName = ans.name ?: return null
                                val path = moveProject.root.toNioPathOrNull() ?: return null
                                return Module(packageName, modName, path)
                            }
                            else -> continue
                        }
                    }
                    null
                }
            }
        }
    }
}

data class MoveCommandLine(
    val command: String,
    val workingDirectory: Path?,
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
)
