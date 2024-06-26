package org.move.cli.runConfigurations.producers.aptos

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.move.cli.Consts
import org.move.cli.MoveProject
import org.move.cli.runConfigurations.CliCommandLineArgs
import org.move.cli.runConfigurations.aptos.AptosCommandConfigurationType
import org.move.cli.runConfigurations.aptos.any.AptosCommandConfigurationFactory
import org.move.cli.runConfigurations.producers.CommandConfigurationProducerBase
import org.move.cli.runConfigurations.producers.CommandLineArgsFromContext
import org.move.cli.settings.moveSettings
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.findMoveProject
import org.move.lang.core.psi.ext.hasTestAttr
import org.move.lang.core.psi.ext.hasTestFunctions
import org.move.lang.moveProject
import org.toml.lang.psi.TomlFile

class AptosTestCommandConfigurationProducer: CommandConfigurationProducerBase() {

    override fun getConfigurationFactory() =
        AptosCommandConfigurationFactory(AptosCommandConfigurationType.getInstance())

    override fun fromLocation(location: PsiElement, climbUp: Boolean): CommandLineArgsFromContext? {
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

    private fun findTestFunction(psi: PsiElement, climbUp: Boolean): CommandLineArgsFromContext? {
        val fn = findElement<MvFunction>(psi, climbUp) ?: return null
        if (!fn.hasTestAttr) return null

        val modName = fn.containingModule?.name ?: return null
        val functionName = fn.name ?: return null

        val confName = "Test $modName::$functionName"

        val subCommand = StringBuilder("move test")
        subCommand.append(" --filter $modName::$functionName")

        subCommand.appendCustomCLIFlags(psi.project)

        val moveProject = fn.moveProject ?: return null
        val rootPath = moveProject.contentRootPath ?: return null
        return CommandLineArgsFromContext(
            fn,
            confName,
            CliCommandLineArgs(
                subCommand.toString(),
                workingDirectory = rootPath,
                environmentVariables = initEnvironmentVariables(psi.project)
            )
        )
    }

    private fun findTestModule(psi: PsiElement, climbUp: Boolean): CommandLineArgsFromContext? {
        val mod = findElement<MvModule>(psi, climbUp) ?: return null
        if (!mod.hasTestFunctions()) return null

        val modName = mod.name ?: return null
        val confName = "Test $modName"
        val subCommand = StringBuilder("move test")
        subCommand.append(" --filter $modName")

        subCommand.appendCustomCLIFlags(psi.project)

        val moveProject = mod.moveProject ?: return null
        val rootPath = moveProject.contentRootPath ?: return null
        return CommandLineArgsFromContext(
            mod,
            confName,
            CliCommandLineArgs(
                subCommand.toString(),
                workingDirectory = rootPath,
                environmentVariables = initEnvironmentVariables(psi.project)
            )
        )
    }

    private fun findTestProject(
        location: PsiFileSystemItem,
        moveProject: MoveProject
    ): CommandLineArgsFromContext? {
        val packageName = moveProject.currentPackage.packageName
        val rootPath = moveProject.contentRootPath ?: return null

        val confName = "Test $packageName"
        val subCommand = StringBuilder("move test")

        subCommand.appendCustomCLIFlags(location.project)

        return CommandLineArgsFromContext(
            location,
            confName,
            CliCommandLineArgs(
                subCommand.toString(),
                workingDirectory = rootPath,
                environmentVariables = initEnvironmentVariables(location.project)
            )
        )
    }

    private fun initEnvironmentVariables(project: Project): EnvironmentVariablesData {
        val environmentMap = linkedMapOf<String, String>()
        if (project.moveSettings.addCompilerV2CLIFlags) {
            environmentMap[Consts.MOVE_COMPILER_V2_ENV] = "true"
        }
        return EnvironmentVariablesData.create(environmentMap, true)
    }

    private fun StringBuilder.appendCustomCLIFlags(project: Project) {
        if (project.moveSettings.skipFetchLatestGitDeps) {
            append(" --skip-fetch-latest-git-deps")
        }
        if (project.moveSettings.dumpStateOnTestFailure) {
            append(" --dump")
        }
        if (project.moveSettings.addCompilerV2CLIFlags) {
            append(" --compiler-version v2 --language-version 2.0")
        }
    }
}
