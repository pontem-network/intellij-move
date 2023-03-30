package org.move.cli.runConfigurations.aptos.run

import com.intellij.openapi.project.Project
import org.move.cli.runConfigurations.aptos.AptosCommandLine
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.allParamsAsBindings
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.ItemFQName
import org.move.lang.index.MvEntryFunctionIndex
import java.nio.file.Path

data class Transaction(
    var functionId: ItemFQName,
    val typeParams: MutableMap<String, String?>,
    val params: MutableMap<String, String?>,
//    var selectedProfile: String? = null,
) {
    fun toAptosCommandLine(workingDirectory: Path?): AptosCommandLine {
        val typeParams = this.typeParams.mapNotNull { it.value }.flatMap { listOf("--type-args", it) }
        val params = this.params.mapNotNull { it.value }.flatMap { listOf("--args", it) }

//        val profile = this.selectedProfile
//        val profileArgs =
//            if (profile != null) listOf("--profile", profile) else listOf()
        val commandArgs = listOf(
//            profileArgs,
            listOf("--function-id", functionId.cmdText()),
            typeParams,
            params
        ).flatten()
        return AptosCommandLine(
            "move run",
            workingDirectory = workingDirectory,
            arguments = commandArgs
        )
    }

    fun commandText(): String = this.toAptosCommandLine(null).joinedCommand()

    companion object {
        fun template(entryFunction: MvFunction): Transaction {
            val typeParameterNames = entryFunction.typeParameters.mapNotNull { it.name }

            val nullTypeParams = mutableMapOf<String, String?>()
            for (typeParameterName in typeParameterNames) {
                nullTypeParams[typeParameterName] = null
            }

            val parameterBindings = entryFunction.allParamsAsBindings.drop(1)
            val parameterNames = parameterBindings.map { it.name }

            val nullParams = mutableMapOf<String, String?>()
            for (parameterName in parameterNames) {
                nullParams[parameterName] = null
            }

            return Transaction(entryFunction.fqName, nullTypeParams, nullParams)
        }

        sealed class Result<T> {
            data class Ok<T>(val value: T) : Result<T>()
            data class Err<T>(val message: String) : Result<T>()
        }

        fun parseFromCommand(project: Project, command: String): Transaction? {
            val runCommandParser =
                RunCommandParser.parse(command) ?: return null
//                RunCommandParser.parse(command) ?: throw ConfigurationException("Malformed command")

            val functionId = ItemFQName.fromCmdText(runCommandParser.functionId)
                ?: return null
//                ?: throw ConfigurationException("Malformed functionId")
            val entryFunction =
                MvEntryFunctionIndex.getFunction(project, functionId.cmdText())
                    ?: return null

            val transaction = template(entryFunction)

            val typeParameterNames = entryFunction.typeParameters.mapNotNull { it.name }
            for ((name, value) in typeParameterNames.zip(runCommandParser.typeArgs)) {
                transaction.typeParams[name] = value
            }

            val parameterBindings = entryFunction.allParamsAsBindings.drop(1)
            val parameterNames = parameterBindings.map { it.name }
            for ((name, value) in parameterNames.zip(runCommandParser.args)) {
                transaction.params[name] = value
            }

            //            val movePackage =
//                entryFunction.moveProject?.currentPackage
//                    ?: return Result.Err("Function does not belong to a package")
//            val profiles =
//                movePackage.aptosConfigYaml?.runProfiles().orEmpty()

            return transaction
        }
    }
}
