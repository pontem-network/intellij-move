package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.lang.core.types.Address

private val MoveModuleDef.importStatements: List<MoveImportStatement>
    get() = moduleBlock?.importStatementList.orEmpty()

fun MoveModuleDef.imports(): Map<Address, MoveImport> {
    val items = mutableMapOf<Address, MoveImport>()
    for (stmt in importStatements) {
        val address = stmt.addressRef?.addressLiteral?.text ?: continue
        items[Address(address)] = stmt.import ?: continue
    }
    return items
}

fun MoveModuleDef.importAliases(): List<MoveImportAlias> = imports().values.flatMap { it.aliases() }

fun MoveModuleDef.functions(): List<MoveFunctionDef> =
    moduleBlock?.functionDefList.orEmpty()

fun MoveModuleDef.nativeFunctions(): List<MoveNativeFunctionDef> =
    moduleBlock?.nativeFunctionDefList.orEmpty()

fun MoveModuleDef.structs(): List<MoveStructDef> =
    moduleBlock?.structDefList.orEmpty()

fun MoveModuleDef.nativeStructs(): List<MoveNativeStructDef> =
    moduleBlock?.nativeStructDefList.orEmpty()

fun MoveModuleDef.consts(): List<MoveConstDef> =
    moduleBlock?.constDefList.orEmpty()

fun MoveModuleDef.schemas(): List<MoveSchemaDef> =
    moduleBlock?.itemSpecDefList.orEmpty().mapNotNull { it.schemaDef }
