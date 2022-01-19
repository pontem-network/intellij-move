package org.move.lang.core.psi.ext

import org.move.lang.MvElementTypes.POST
import org.move.lang.core.psi.MvLetSpecStatement
import org.move.lang.core.psi.MvSchemaVarDeclStatement
import org.move.lang.core.psi.MvSpecBlock

fun MvSpecBlock.schemaVars(): List<MvSchemaVarDeclStatement> = childrenOfType()

fun MvSpecBlock.letStatements(): List<MvLetSpecStatement> = this.childrenOfType()

fun MvSpecBlock.letStatements(post: Boolean): List<MvLetSpecStatement> {
    val statements = this.letStatements()
    return if (post) {
        statements.filter { it.isPost }
    } else {
        statements.filter { !it.isPost }
    }
}
