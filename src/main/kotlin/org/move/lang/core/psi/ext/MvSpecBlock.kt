package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvLetStatement
import org.move.lang.core.psi.MvSchemaFieldStatement
import org.move.lang.core.psi.MvSpecBlock

fun MvSpecBlock.schemaFields(): List<MvSchemaFieldStatement> = childrenOfType()

fun MvSpecBlock.letStatements(): List<MvLetStatement> = this.childrenOfType()

fun MvSpecBlock.letStatements(post: Boolean): List<MvLetStatement> {
    val statements = this.letStatements()
    return if (post) {
        statements.filter { it.isPost }
    } else {
        statements.filter { !it.isPost }
    }
}
