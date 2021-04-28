package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveFunctionSignature
import org.move.lang.core.psi.MoveFunctionVisibilityModifier
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.MoveScriptDef

enum class FunctionVisibility {
    PRIVATE,
    PUBLIC,
    PUBLIC_FRIEND,
    PUBLIC_SCRIPT;
}

val MoveFunctionSignature.visibility: FunctionVisibility
    get() {
        val visibility =
            this.getPrevNonCommentSibling() as? MoveFunctionVisibilityModifier
                ?: return FunctionVisibility.PRIVATE
        return when (visibility.node.text) {
            "public" -> FunctionVisibility.PUBLIC
            "public(friend)" -> FunctionVisibility.PUBLIC_FRIEND
            "public(script)" -> FunctionVisibility.PUBLIC_SCRIPT
            else -> FunctionVisibility.PRIVATE
        }
    }

//val MoveFunctionSignature.function: MoveFunctionDef?
//    get() {
//        return this.parent as? MoveFunctionDef
//    }
//
//val MoveFunctionSignature.nativeFunction: MoveNativeDef?
//    get() {
//        return this.parent as? MoveNativeDef
//    }

val MoveFunctionSignature.module: MoveModuleDef?
    get() {
        val moduleBlock = this.parent.parent
        return moduleBlock.parent as? MoveModuleDef
    }

val MoveFunctionSignature.script: MoveScriptDef?
    get() {
        val scriptBlock = this.parent.parent
        return scriptBlock.parent as? MoveScriptDef
    }
