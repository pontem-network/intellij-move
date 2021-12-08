package org.move.cli.runconfig

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvFunctionDef
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.lang.core.psi.ext.isTest
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull
import java.nio.file.Path

class MoveTestRunConfigurationProducer : LazyRunConfigurationProducer<MoveRunConfiguration>() {

    override fun getConfigurationFactory() = MoveRunConfigurationType.getInstance()

    override fun setupConfigurationFromContext(
        templateConfiguration: MoveRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val testConfig = FunctionTestConfig.fromLocation(sourceElement.get()) ?: return false
        val cmd = MoveCommandLine(
            "package test --filter ${testConfig.functionName}",
            testConfig.path
        )
        templateConfiguration.name = testConfig.testName()
        templateConfiguration.cmd = cmd
        return true
    }

    override fun isConfigurationFromContext(
        configuration: MoveRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val location = context.psiLocation ?: return false
        val testConfig = FunctionTestConfig.fromLocation(location) ?: return false
        val expectedCmd = MoveCommandLine(
            "package test --filter ${testConfig.functionName}",
            testConfig.path
        )
        return configuration.name == testConfig.testName()
                && configuration.cmd == expectedCmd
    }
}


data class FunctionTestConfig(val functionName: String, val modName: String, val path: Path) {

    fun testName(): String = "Test $modName::$functionName"

    companion object {
        fun fromLocation(location: PsiElement): FunctionTestConfig? {
            val testFunction = getTestFunction(location) ?: return null
            val functionName = testFunction.functionSignature?.name ?: return null
            val modName = testFunction.containingModule?.name ?: return null
            val path = testFunction.moveProject()?.root?.toNioPathOrNull() ?: return null
            return FunctionTestConfig(functionName, modName, path)
        }

        private fun getTestFunction(location: PsiElement): MvFunctionDef? {
            val testFunction = location.ancestorOrSelf<MvFunctionDef>() ?: return null
            if (!testFunction.isTest) return null
            return testFunction
        }
    }
}

data class MoveCommandLine(val command: String, val workingDirectory: Path?)
