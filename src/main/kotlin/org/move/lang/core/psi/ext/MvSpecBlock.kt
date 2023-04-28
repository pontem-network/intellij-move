package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

fun MvSpecCodeBlock.schemaFields(): List<MvSchemaFieldStmt> = childrenOfType()

val MvSpecCodeBlock.allLetStmts: List<MvLetStmt> get() = this.childrenOfType()

fun MvSpecCodeBlock.letStmts(post: Boolean): List<MvLetStmt> {
    val letStmts = this.allLetStmts
    return if (post) {
        letStmts.filter { it.isPost }
    } else {
        letStmts.filter { !it.isPost }
    }
}

fun MvSpecCodeBlock.inlineFunctions(): List<MvSpecInlineFunction> {
    return this.childrenOfType<MvSpecInlineFunctionStmt>()
        .map { it.specInlineFunction }
}
