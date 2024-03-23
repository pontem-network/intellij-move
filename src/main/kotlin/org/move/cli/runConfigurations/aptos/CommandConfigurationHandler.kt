package org.move.cli.runConfigurations.aptos

import com.intellij.psi.PsiElement
import org.move.cli.MoveProject
import org.move.cli.runConfigurations.CliCommandLineArgs
import org.move.cli.runConfigurations.producers.CommandConfigurationProducerBase
import org.move.cli.runConfigurations.producers.CommandLineArgsFromContext
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvFunctionParameter
import org.move.lang.core.psi.ext.functionId
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.inference
import org.move.lang.moveProject
import org.move.stdext.RsResult

abstract class CommandConfigurationHandler {

    abstract val subCommand: String

    abstract fun functionPredicate(function: MvFunction): Boolean

    abstract fun configurationName(functionId: String): String

    fun configurationFromLocation(location: PsiElement): CommandLineArgsFromContext? {
        val function =
            CommandConfigurationProducerBase.findElement<MvFunction>(location, true)
                ?.takeIf(this::functionPredicate)
                ?: return null
        val moveProject = function.moveProject ?: return null

        val functionId = function.functionId(moveProject) ?: return null
        val profileName = moveProject.profiles.firstOrNull()
        val workingDirectory = moveProject.contentRootPath

        val arguments = mutableListOf<String>()
        if (profileName != null) {
            arguments.addAll(listOf("--profile", profileName))
        }
        arguments.addAll(listOf("--function-id", functionId))

        val commandLine = CliCommandLineArgs(subCommand, arguments, workingDirectory)
        return CommandLineArgsFromContext(
            function,
            configurationName(functionId),
            commandLine
        )
    }

    abstract fun getFunctionCompletionVariants(moveProject: MoveProject): Collection<String>

    abstract fun getFunctionItem(moveProject: MoveProject, functionQualName: String): MvFunction?

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
        val command = commandArguments.joinToString(" ")
        println("Command is $command")
        return RsResult.Ok(command)
    }

    fun parseCommand(
        moveProject: MoveProject,
        command: String
    ): RsResult<Pair<String, FunctionCall>, String> {
        val res = FunctionCallParser.parse(command, subCommand)
        val callArgs = when (res) {
            is RsResult.Ok -> res.ok
            is RsResult.Err -> return RsResult.Err("malformed command error '${res.err}'")
        }
        val profileName = callArgs.profile

        val functionId = callArgs.functionId
        val function = getFunctionByCmdName(moveProject, functionId)
            ?: return RsResult.Err("function with this functionId does not exist in the current project")

//        val aptosConfig = moveProject.aptosConfigYaml
//        if (aptosConfig == null) {
//            return RsResult.Err("Aptos account is not initialized / is invalid for the current project")
//        }
//
//        if (profileName !in aptosConfig.profiles) {
//            return RsResult.Err("account '$profileName' is not present in the project's accounts")
//        }
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
