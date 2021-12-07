package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvFunctionSignature
import org.move.lang.core.psi.MvFunctionVisibilityModifier
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.MvScriptDef

enum class FunctionVisibility {
    PRIVATE,
    PUBLIC,
    PUBLIC_FRIEND,
    PUBLIC_SCRIPT;
}

val MvFunctionSignature.visibility: FunctionVisibility
    get() {
        val visibility =
            this.getPrevNonCommentSibling() as? MvFunctionVisibilityModifier
                ?: return FunctionVisibility.PRIVATE
        return when (visibility.node.text) {
            "public" -> FunctionVisibility.PUBLIC
            "public(friend)" -> FunctionVisibility.PUBLIC_FRIEND
            "public(script)" -> FunctionVisibility.PUBLIC_SCRIPT
            else -> FunctionVisibility.PRIVATE
        }
    }

//val MvFunctionSignature.function: MvFunctionDef?
//    get() {
//        return this.parent as? MvFunctionDef
//    }
//
//val MvFunctionSignature.nativeFunction: MvNativeDef?
//    get() {
//        return this.parent as? MvNativeDef
//    }

val MvFunctionSignature.module: MvModuleDef?
    get() {
        val moduleBlock = this.parent.parent
        return moduleBlock.parent as? MvModuleDef
    }

val MvFunctionSignature.script: MvScriptDef?
    get() {
        val scriptBlock = this.parent.parent
        return scriptBlock.parent as? MvScriptDef
    }
