package org.move.ide.hints

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.ext.name
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.*

class CallInfo(
    val selfParameter: String?,
    val parameters: List<Parameter>
) {
    class Parameter(val name: String?, val type: MvType?, val ty: Ty? = null) {
        fun renderType(): String {
            return if (ty != null && ty !is TyUnknown) {
                tyToString(ty)
            } else {
                type?.text ?: "_"
            }
        }
    }

    companion object {
        fun resolve(callExpr: MvCallExpr): CallInfo? {
            val msl = callExpr.isMsl()
            val callTy = callExpr.inference(msl)?.getCallableType(callExpr) as? TyFunction ?: return null
            val item = callExpr.path.reference?.resolveFollowingAliases() ?: return null
            return buildFunctionParameters(item, callTy)
        }

        fun resolve(assertMacroExpr: MvAssertMacroExpr): CallInfo {
            return CallInfo(null, buildList {
                add(Parameter("_", null, TyBool))
                add(Parameter("err", null, TyInteger(TyInteger.Kind.u64)))
            })
        }

        fun resolve(methodCall: MvMethodCall): CallInfo? {
            val fn = methodCall.reference.resolveFollowingAliases() as? MvFunction ?: return null
            val msl = methodCall.isMsl()
            val callTy = methodCall.inference(msl)?.getCallableType(methodCall) as? TyFunction ?: return null
            return buildFunctionParameters(fn, callTy)
        }

        private fun buildFunctionParameters(item: MvElement, ty: TyFunction): CallInfo? {
            return when (item) {
                is MvFunction -> {
                    val tys = ty.paramTypes.drop(if (item.isMethod) 1 else 0)
                    val params = item.parameters
                        .drop(if (item.isMethod) 1 else 0).map { it.name to it.type }
                    val self = item.selfParam?.let {
                        ty.paramTypes.firstOrNull()?.let { "self: ${tyToString(it)}" } ?: "_"
                    }
                    CallInfo(self, buildParameters(tys, params))
                }
                is MvStruct, is MvEnumVariant -> {
                    // tuple struct
                    val tys = ty.paramTypes
                    CallInfo(null, tys.map { Parameter(null, null, it) })
                }
                else -> null
            }
        }

        private fun buildParameters(
            argumentTys: List<Ty>,
            parameterTypes: List<Pair<String?, MvType?>>,
        ): List<Parameter> {
            return argumentTys.zip(parameterTypes).map { (ty, param) ->
                val (name, parameterType) = param
                Parameter(name, parameterType, ty)
            }
        }
    }

//    private constructor(fn: MvFunction, parameters: List<Parameter>): this(
//        fn.selfParam?.let { self ->
//            buildString {
////                if (self.isRef) append("&")
////                if (self.mutability.isMut) append("mut ")
//                append("self: ${fn.selfParamTy()}")
//            }
//        },
//        parameters
//    )

//    private constructor(parameters: List<Parameter>) : this(
//        null,
//        parameters
//    )
}