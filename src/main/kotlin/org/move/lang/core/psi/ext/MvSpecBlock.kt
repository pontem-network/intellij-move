package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

fun MvSpecBlock.schemaFields(): List<MvSchemaFieldStmt> = childrenOfType()

fun MvSpecBlock.letStmts(): List<MvLetStmt> = this.childrenOfType()

fun MvSpecBlock.letStmts(post: Boolean): List<MvLetStmt> {
    val statements = this.letStmts()
    return if (post) {
        statements.filter { it.isPost }
    } else {
        statements.filter { !it.isPost }
    }
}

fun MvSpecBlock.inlineFunctions(): List<MvSpecInlineFunction> {
    return this.childrenOfType<MvSpecInlineFunctionStmt>()
        .map { it.specInlineFunction }
}
