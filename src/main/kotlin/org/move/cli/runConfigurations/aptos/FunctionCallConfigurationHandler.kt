package org.move.cli.runConfigurations.aptos

import com.intellij.psi.PsiElement
import org.move.cli.MoveProject
import org.move.cli.runConfigurations.producers.CommandConfigurationProducerBase
import org.move.cli.runConfigurations.producers.CommandLineFromContext
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvFunctionParameter
import org.move.lang.core.psi.ext.functionId
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.inference
import org.move.lang.moveProject
import org.move.stdext.RsResult

abstract class FunctionCallConfigurationHandler {

    abstract val subCommand: String

    abstract fun functionPredicate(function: MvFunction): Boolean

    abstract fun configurationName(functionId: String): String

    fun configurationFromLocation(location: PsiElement): CommandLineFromContext? {
        val function =
            CommandConfigurationProducerBase.findElement<MvFunction>(location, true)
                ?.takeIf(this::functionPredicate)
                ?: return null
        val functionId = function.functionId() ?: return null

        val moveProject = function.moveProject ?: return null
        val profileName = moveProject.profiles.firstOrNull()
        val workingDirectory = moveProject.contentRootPath

        val arguments = mutableListOf<String>()
        if (profileName != null) {
            arguments.addAll(listOf("--profile", profileName))
        }
        arguments.addAll(listOf("--function-id", functionId))

        val commandLine = AptosCommandLine(subCommand, arguments, workingDirectory)
        return CommandLineFromContext(
            function,
            configurationName(functionId),
            commandLine
        )
    }

    abstract fun getFunctionCompletionVariants(moveProject: MoveProject): Collection<String>

    abstract fun getFunction(moveProject: MoveProject, functionQualName: String): MvFunction?

    abstract fun getFunctionByCmdName(moveProject: MoveProject, functionCmdName: String): MvFunction?

    abstract fun getFunctionParameters(function: MvFunction): List<MvFunctionParameter>

    fun generateCommand(
        moveProject: MoveProject,
        profileName: String,
        functionCall: FunctionCall
    ): RsResult<String, String> {
        val functionId = functionCall.functionId(moveProject) ?: return RsResult.Err("FunctionId is null")

        val typeParams = functionCall.typeParams
            .mapNotNull { it.value }.flatMap { listOf("--type-args", it) }
        val params = functionCall.valueParams
            .mapNotNull { it.value?.cmdText() }.flatMap { listOf("--args", it) }

        val commandArguments = listOf(
            subCommand.split(' '),
            listOf("--profile", profileName),
            listOf("--function-id", functionId),
            typeParams,
            params
        ).flatten()
        return RsResult.Ok(commandArguments.joinToString(" "))
    }

    fun parseCommand(
        moveProject: MoveProject,
        command: String
    ): RsResult<Pair<String, FunctionCall>, String> {
        val callArgs = FunctionCallParser.parse(command, subCommand)
            ?: return RsResult.Err("malformed command arguments")

        val profileName = callArgs.profile

        val functionId = callArgs.functionId
        val function = getFunctionByCmdName(moveProject, functionId)
            ?: return RsResult.Err("function with this functionId does not exist in the current project")

        val aptosConfig = moveProject.aptosConfigYaml
        if (aptosConfig == null) {
            return RsResult.Err("aptos account is not initialized for the current project")
//            Notifications.pluginNotifications()
//                .createNotification(
//                    "Aptos account is not initialized for the current project",
//                    NotificationType.WARNING
//                )
//                .addAction(InitializeAptosAccountAction("Configure"))
//                .notify(project)
//            return null
        }

        if (profileName !in aptosConfig.profiles) {
            return RsResult.Err("Account used in the command is not part of project accounts")
        }
        val transaction = FunctionCall.template(function)

        val typeParameterNames = function.typeParameters.mapNotNull { it.name }
        for ((name, value) in typeParameterNames.zip(callArgs.typeArgs)) {
            transaction.typeParams[name] = value
        }

        val parameterBindings = getFunctionParameters(function).map { it.bindingPat }
        val inference = function.inference(false)
        for ((binding, valueWithType) in parameterBindings.zip(callArgs.args)) {
            val name = binding.name
            val value = valueWithType.split(':')[1]
            val ty = inference.getPatType(binding)
            transaction.valueParams[name] = FunctionCallParam(value, FunctionCallParam.tyTypeName(ty))
        }

        return RsResult.Ok(Pair(profileName, transaction))
    }
}
