package org.move.cli.runConfigurations.aptos

import com.intellij.openapi.project.Project
import org.move.cli.MoveProject
import org.move.cli.moveProjects
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.allParamsAsBindings
import org.move.lang.core.psi.ext.transactionParameters
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.ItemQualName
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.*
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

data class FunctionCallParam(val value: String, val type: String) {
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


data class FunctionCall(
    var functionId: ItemQualName?,
    var profile: Profile?,
    var typeParams: MutableMap<String, String?>,
    var params: MutableMap<String, FunctionCallParam?>,
) {
    fun toAptosCommandLine(subCommand: String): AptosCommandLine? {
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
            subCommand,
            workingDirectory = workDir,
            arguments = commandArgs
        )
    }

    fun hasRequiredParameters(): Boolean {
        val function = functionId?.item as? MvFunction ?: return true
        // TODO: view function does not have first signer as a param
        return function.typeParameters.isNotEmpty()
                || function.transactionParameters.isNotEmpty()
    }

    companion object {
        fun empty(profile: Profile?): FunctionCall {
            return FunctionCall(null, profile, mutableMapOf(), mutableMapOf())
        }

        fun template(profile: Profile, function: MvFunction): FunctionCall {
            val typeParameterNames = function.typeParameters.mapNotNull { it.name }

            val nullTypeParams = mutableMapOf<String, String?>()
            for (typeParameterName in typeParameterNames) {
                nullTypeParams[typeParameterName] = null
            }

            val parameterBindings = function.allParamsAsBindings.drop(1)
            val parameterNames = parameterBindings.map { it.name }

            val nullParams = mutableMapOf<String, FunctionCallParam?>()
            for (parameterName in parameterNames) {
                nullParams[parameterName] = null
            }

            val qualName = function.qualName ?: error("qualName should not be null, checked before")
            return FunctionCall(qualName, profile, nullTypeParams, nullParams)
        }

        fun parseFromCommand(
            project: Project,
            command: String,
            workingDirectory: Path?,
            getFunction: (Project, String) -> MvFunction?
        ): FunctionCall? {
            val callArgs =
                FunctionCallParser.parse(command) ?: return null

            val profileName = callArgs.profile
            val moveProject = workingDirectory?.let { project.moveProjects.findMoveProject(it) }
            if (moveProject == null) {
                TODO("Try all projects for their profiles, use the first one")
            }
            val functionId = callArgs.functionId
            val (addressValue, _, _) = ItemQualName.split(functionId) ?: return null
            val namedAddresses = moveProject.getAddressNamesForValue(addressValue)

            var function = getFunction(project, functionId)
            if (function == null) {
                function = namedAddresses
                    .map {
                        val modifiedFunctionId = functionId.replace(addressValue, it)
                        getFunction(project, modifiedFunctionId)
                    }
                    .firstOrNull() ?: return null
            }

            val config = moveProject.aptosConfigYaml ?: TODO("Project is not `aptos init`ed")
            if (profileName !in config.profiles) {
                TODO("Invalid profile, use the default one, or first one if there's no default")
            }
            val transaction = template(Profile(profileName, moveProject), function)

            val typeParameterNames = function.typeParameters.mapNotNull { it.name }
            for ((name, value) in typeParameterNames.zip(callArgs.typeArgs)) {
                transaction.typeParams[name] = value
            }

            val parameterBindings = function.allParamsAsBindings.drop(1)
            val inference = function.inference(false)
//            val inferenceCtx = function.inferenceContext(false)
            for ((binding, value) in parameterBindings.zip(callArgs.args)) {
                val name = binding.name
                val ty = inference.getPatType(binding)
//                val ty = inferenceCtx.getBindingPatTy(binding)
                transaction.params[name] = FunctionCallParam(value, FunctionCallParam.tyTypeName(ty))
            }

            return transaction
        }
    }
}
