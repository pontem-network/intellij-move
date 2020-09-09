package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveImport
import org.move.lang.core.psi.MoveImportAlias
import org.move.lang.core.psi.MoveImportStatement
import org.move.lang.core.psi.MoveScriptDef
import org.move.lang.core.types.Address

private val MoveScriptDef.importStatements: List<MoveImportStatement>
    get() = scriptBlock?.importStatementList.orEmpty()

fun MoveScriptDef.imports(): Map<Address, MoveImport> {
    val items = mutableMapOf<Address, MoveImport>()
    for (stmt in importStatements) {
        val address = stmt.addressRef?.addressLiteral?.text ?: continue
        items[Address(address)] = stmt.import ?: continue
    }
    return items
}

fun MoveScriptDef.importAliases(): List<MoveImportAlias> = imports().values.flatMap { it.aliases() }
