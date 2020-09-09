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
    this.moduleBlock?.functionDefList.orEmpty()

fun MoveModuleDef.nativeFunctions(): List<MoveNativeFunctionDef> =
    this.moduleBlock?.nativeFunctionDefList.orEmpty()

fun MoveModuleDef.structs(): List<MoveStructDef> =
    this.moduleBlock?.structDefList.orEmpty()

fun MoveModuleDef.nativeStructs(): List<MoveNativeStructDef> =
    this.moduleBlock?.nativeStructDefList.orEmpty()

fun MoveModuleDef.consts(): List<MoveConstDef> =
    this.moduleBlock?.constDefList.orEmpty()

fun MoveModuleDef.schemas(): List<MoveSchemaDef> =
    this.moduleBlock?.schemaDefList.orEmpty()
