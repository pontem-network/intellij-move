package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*

fun MoveScriptDef.consts(): List<MoveConstDef> =
    scriptBlock?.constDefList.orEmpty()

fun MoveScriptDef.builtinFunctions(): List<MoveNativeFunctionDef> =
    listOf(
        createBuiltinFunc("native fun assert(_: bool, err: u64): ();", project)
    )

abstract class MoveScriptDefMixin(node: ASTNode) : MoveElementImpl(node),
                                                   MoveScriptDef {
    override val importStatements: List<MoveImportStatement>
        get() =
            this.scriptBlock?.importStatementList.orEmpty()
}