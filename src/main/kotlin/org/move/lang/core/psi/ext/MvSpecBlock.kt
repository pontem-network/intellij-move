package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

fun MvSpecCodeBlock.schemaFields(): List<MvSchemaFieldStmt> = childrenOfType()

fun MvSpecCodeBlock.globalVariables(): List<MvGlobalVariableStmt> = childrenOfType()

val MvSpecCodeBlock.allLetStmts: List<MvLetStmt> get() = this.childrenOfType()

fun MvSpecCodeBlock.specInlineFunctions(): List<MvSpecInlineFunction> {
    return this.childrenOfType<MvSpecInlineFunctionStmt>()
        .map { it.specInlineFunction }
}
