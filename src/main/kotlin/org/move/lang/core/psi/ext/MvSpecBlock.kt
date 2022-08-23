package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

fun MvItemSpecBlock.schemaFields(): List<MvSchemaFieldStmt> = childrenOfType()

fun MvItemSpecBlock.letStmts(): List<MvLetStmt> = this.childrenOfType()

fun MvItemSpecBlock.letStmts(post: Boolean): List<MvLetStmt> {
    val statements = this.letStmts()
    return if (post) {
        statements.filter { it.isPost }
    } else {
        statements.filter { !it.isPost }
    }
}

fun MvItemSpecBlock.inlineFunctions(): List<MvSpecInlineFunction> {
    return this.childrenOfType<MvSpecInlineFunctionStmt>()
        .map { it.specInlineFunction }
}
