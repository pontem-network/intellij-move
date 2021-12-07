package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*

fun MvScriptDef.allFnSignatures(): List<MvFunctionSignature> {
    val block = scriptBlock ?: return emptyList()
    return block.functionDefList.mapNotNull { it.functionSignature }
}

fun MvScriptDef.constBindings(): List<MvBindingPat> =
    scriptBlock?.constDefList.orEmpty().mapNotNull { it.bindingPat }

fun MvScriptDef.builtinScriptFnSignatures(): List<MvFunctionSignature> {
    return listOfNotNull(
        createBuiltinFuncSignature("native fun assert(_: bool, err: u64);", project)
    )
}

//fun MvScriptDef.builtinFunctions(): List<MvNativeFunctionDef> =
//    listOf(
//        createBuiltinFuncSignature("native fun assert(_: bool, err: u64): ();", project)
//    )

abstract class MvScriptDefMixin(node: ASTNode) : MvElementImpl(node),
                                                   MvScriptDef {
    override val importStatements: List<MvImportStatement>
        get() =
            this.scriptBlock?.importStatementList.orEmpty()
}
