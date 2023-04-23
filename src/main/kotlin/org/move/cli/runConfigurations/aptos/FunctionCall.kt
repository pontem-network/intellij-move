package org.move.cli.runConfigurations.aptos

import org.move.cli.MoveProject
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.allParamsAsBindings
import org.move.lang.core.psi.ext.isEntry
import org.move.lang.core.psi.ext.isView
import org.move.lang.core.psi.ext.transactionParameters
import org.move.lang.core.psi.parameters
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.ty.*

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

data class CommandError(val message: String)

data class FunctionCall(
    val item: MvFunction,
    val typeParams: MutableMap<String, String?>,
    val valueParams: MutableMap<String, FunctionCallParam?>
) {
    fun itemName(): String? = item.qualName?.editorText()
    fun functionId(moveProject: MoveProject): String? = item.qualName?.cmdText(moveProject)

    fun typeParamsLabelText(): String =
        this.typeParams.map { (k, v) -> "$k=${v ?: ""}" }.joinToString()

    fun valueParamsLabelText(): String =
        this.valueParams.map { (k, v) -> "$k=${v?.value ?: ""}" }.joinToString()

//    fun toAptosCommandLine(subCommand: String): RsResult<AptosCommandLine, CommandError> {
//        val profile = this.profile ?: return RsResult.Err(CommandError("Profile is null"))
//        val functionId =
//            this.functionId?.cmdText(profile.moveProject)
//                ?: return RsResult.Err(CommandError("FunctionId is null"))
//
//        val typeParams = this.typeParams.mapNotNull { it.value }.flatMap { listOf("--type-args", it) }
//        val params = this.valueParams.mapNotNull { it.value?.cmdText() }.flatMap { listOf("--args", it) }
//
//        val workDir = profile.moveProject.contentRootPath
//        val commandArgs = listOf(
//            listOf("--profile", profile.name),
//            listOf("--function-id", functionId),
//            typeParams,
//            params
//        ).flatten()
//        return RsResult.Ok(
//            AptosCommandLine(
//                subCommand,
//                workingDirectory = workDir,
//                arguments = commandArgs
//            )
//        )
//    }

    fun functionHasParameters(): Boolean {
        val fn = item
        return when {
            fn.isView -> fn.typeParameters.isNotEmpty() || fn.parameters.isNotEmpty()
            fn.isEntry -> fn.typeParameters.isNotEmpty() || fn.transactionParameters.isNotEmpty()
            else -> true
        }
    }

    companion object {
//        fun empty(profile: Profile?): FunctionCall {
//            return FunctionCall(null, profile, mutableMapOf(), mutableMapOf())
//        }

        fun template(function: MvFunction): FunctionCall {
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
            return FunctionCall(function, nullTypeParams, nullParams)
        }

//        fun parse(
//            moveProject: MoveProject,
//            command: String,
//            getFunction: (MoveProject, String) -> MvFunction?
//        ): FunctionCall? {
//            val project = moveProject.project
//            val callArgs = FunctionCallParser.parse(command) ?: return null
//
//            val profileName = callArgs.profile
////            val moveProject = workingDirectory?.let { project.moveProjects.findMoveProject(it) }
////            if (moveProject == null) {
////                TODO("Try all projects for their profiles, use the first one")
////            }
//            val functionId = callArgs.functionId
//            val function = getFunction(moveProject, functionId) ?: return null
////            if (function == null) {
////                val addressValue = ItemQualName.split(functionId)?.first ?: return null
////                val namedAddresses = moveProject.getAddressNamesForValue(addressValue)
////                function = namedAddresses
////                    .map {
////                        val modifiedFunctionId = functionId.replace(addressValue, it)
////                        getFunction(moveProject, modifiedFunctionId)
////                    }
////                    .firstOrNull() ?: return null
////            }
//
//            val aptosConfig = moveProject.aptosConfigYaml
//            if (aptosConfig == null) {
//                Notifications.pluginNotifications()
//                    .createNotification(
//                        "Aptos account is not initialized for the current project",
//                        NotificationType.WARNING
//                    )
//                    .addAction(InitializeAptosAccountAction("Configure"))
//                    .notify(project)
//                return null
//            }
//
//            if (profileName !in aptosConfig.profiles) {
//                TODO("Invalid profile, use the default one, or first one if there's no default")
//            }
//            val transaction = template(Profile(profileName, moveProject), function)
//
//            val typeParameterNames = function.typeParameters.mapNotNull { it.name }
//            for ((name, value) in typeParameterNames.zip(callArgs.typeArgs)) {
//                transaction.typeParams[name] = value
//            }
//
//            val parameterBindings = function.allParamsAsBindings.drop(1)
//            val inference = function.inference(false)
////            val inferenceCtx = function.inferenceContext(false)
//            for ((binding, value) in parameterBindings.zip(callArgs.args)) {
//                val name = binding.name
//                val ty = inference.getPatType(binding)
////                val ty = inferenceCtx.getBindingPatTy(binding)
//                transaction.valueParams[name] = FunctionCallParam(value, FunctionCallParam.tyTypeName(ty))
//            }
//
//            return transaction
//        }
    }
}
