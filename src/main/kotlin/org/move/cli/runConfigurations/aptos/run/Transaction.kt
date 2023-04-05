package org.move.cli.runConfigurations.aptos.run

import com.intellij.openapi.project.Project
import org.move.cli.MoveProject
import org.move.cli.moveProjects
import org.move.cli.runConfigurations.aptos.AptosCommandLine
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.allParamsAsBindings
import org.move.lang.core.psi.ext.transactionParameters
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.ItemQualName
import org.move.lang.core.types.infer.inferenceContext
import org.move.lang.core.types.ty.*
import org.move.lang.index.MvEntryFunctionIndex
import java.nio.file.Path

data class Profile(
    val name: String,
    val moveProject: MoveProject,
) {
    override fun toString(): String {
        val packageIdent =
            moveProject.currentPackage.packageName.takeIf { it.isNotBlank() }
                ?: this.moveProject.contentRoot.name
        return "$name ($packageIdent)"
    }
}

data class TransactionParam(val value: String, val type: String) {
    fun cmdText(): String = "$type:$value"

    companion object {
        fun tyTypeName(ty: Ty): String {
            return when (ty) {
                is TyInteger -> ty.kind.name
                is TyAddress -> "address"
                is TyBool -> "bool"
                is TyVector -> "vector"
                else -> "unknown"
            }
        }
    }
}


data class Transaction(
    var functionId: ItemQualName?,
    var profile: Profile?,
    var typeParams: MutableMap<String, String?>,
    var params: MutableMap<String, TransactionParam?>,
) {
    fun toAptosCommandLine(): AptosCommandLine? {
        val profile = this.profile ?: return null
        val functionId = this.functionId?.cmdText(profile.moveProject) ?: return null

        val typeParams = this.typeParams.mapNotNull { it.value }.flatMap { listOf("--type-args", it) }
        val params = this.params.mapNotNull { it.value?.cmdText() }.flatMap { listOf("--args", it) }

        val workDir = profile.moveProject.contentRootPath
        val commandArgs = listOf(
            listOf("--profile", profile.name),
            listOf("--function-id", functionId),
            typeParams,
            params
        ).flatten()
        return AptosCommandLine(
            "move run",
            workingDirectory = workDir,
            arguments = commandArgs
        )
    }

    fun hasRequiredParameters(): Boolean {
        val entryFunction = functionId?.item as? MvFunction ?: return true
        return entryFunction.typeParameters.isNotEmpty()
                || entryFunction.transactionParameters.isNotEmpty()
    }

    companion object {
        fun empty(profile: Profile?): Transaction {
            return Transaction(null, profile, mutableMapOf(), mutableMapOf())
        }

        fun template(profile: Profile, entryFunction: MvFunction): Transaction {
            val typeParameterNames = entryFunction.typeParameters.mapNotNull { it.name }

            val nullTypeParams = mutableMapOf<String, String?>()
            for (typeParameterName in typeParameterNames) {
                nullTypeParams[typeParameterName] = null
            }

            val parameterBindings = entryFunction.allParamsAsBindings.drop(1)
            val parameterNames = parameterBindings.map { it.name }

            val nullParams = mutableMapOf<String, TransactionParam?>()
            for (parameterName in parameterNames) {
                nullParams[parameterName] = null
            }

            val qualName = entryFunction.qualName ?: error("qualName should not be null, checked before")
            return Transaction(qualName, profile, nullTypeParams, nullParams)
        }

        sealed class Result<T> {
            data class Ok<T>(val value: T) : Result<T>()
            data class Err<T>(val message: String) : Result<T>()
        }

        fun parseFromCommand(project: Project, command: String, workingDirectory: Path?): Transaction? {
            val runCommandParser =
                RunCommandParser.parse(command) ?: return null

            val profileName = runCommandParser.profile
            val moveProject = workingDirectory?.let { project.moveProjects.findMoveProject(it) }
            if (moveProject == null) {
                TODO("Try all projects for their profiles, use the first one")
            }
            val functionId = runCommandParser.functionId
            val (addressValue, _, _) = ItemQualName.split(functionId) ?: return null
            val namedAddresses = moveProject.getAddressNamesForValue(addressValue)

            var entryFunction = MvEntryFunctionIndex.getEntryFunction(project, functionId)
            if (entryFunction == null) {
                entryFunction = namedAddresses
                    .map {
                        val modifiedFunctionId = functionId.replace(addressValue, it)
                        MvEntryFunctionIndex.getEntryFunction(project, modifiedFunctionId)
                    }
                    .firstOrNull() ?: return null
            }

            val config = moveProject.aptosConfigYaml ?: TODO("Project is not `aptos init`ed")
            if (profileName !in config.profiles) {
                TODO("Invalid profile, use the default one, or first one if there's no default")
            }
            val transaction = template(Profile(profileName, moveProject), entryFunction)

            val typeParameterNames = entryFunction.typeParameters.mapNotNull { it.name }
            for ((name, value) in typeParameterNames.zip(runCommandParser.typeArgs)) {
                transaction.typeParams[name] = value
            }

            val parameterBindings = entryFunction.allParamsAsBindings.drop(1)
            val inferenceCtx = entryFunction.inferenceContext(false)
            for ((binding, value) in parameterBindings.zip(runCommandParser.args)) {
                val name = binding.name
                val ty = inferenceCtx.getBindingPatTy(binding)
                transaction.params[name] = TransactionParam(value, TransactionParam.tyTypeName(ty))
            }

            return transaction
        }
    }
}
