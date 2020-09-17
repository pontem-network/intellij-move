package org.move.lang.core.psi.ext

import com.intellij.openapi.project.Project
import org.move.lang.core.psi.*
import org.move.lang.core.psi.mixins.MoveNativeFunctionDefMixin
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

fun createBuiltinFunc(text: String, project: Project): MoveNativeFunctionDef {
    val function =
        MovePsiFactory(project).createNativeFunctionDef(text)
    (function as MoveNativeFunctionDefMixin).builtin = true
    return function
}

fun MoveModuleDef.nativeFunctions(): List<MoveNativeFunctionDef> {
    val block = moduleBlock ?: return emptyList()

    val builtins = listOf(
        createBuiltinFunc("native fun move_from<R: resource>(addr: address): R;", project),
        createBuiltinFunc("native fun move_to<R: resource>(addr: address, res: R): ();", project),
        createBuiltinFunc("native fun borrow_global<R: resource>(addr: address): &R;", project),
        createBuiltinFunc("native fun borrow_global_mut<R: resource>(addr: address): &mut R;", project),
        createBuiltinFunc("native fun exists<R: resource>(addr: address): bool;", project),
        createBuiltinFunc("native fun freeze<S>(mut_ref: &mut S): &S;", project),
        createBuiltinFunc("native fun assert(predicate: bool, error_code: u64): ();", project),
    )
    return listOf(
        block.nativeFunctionDefList,
        builtins
    ).flatten()
}


fun MoveModuleDef.structs(): List<MoveStructDef> =
    moduleBlock?.structDefList.orEmpty()

fun MoveModuleDef.nativeStructs(): List<MoveNativeStructDef> =
    moduleBlock?.nativeStructDefList.orEmpty()

fun MoveModuleDef.consts(): List<MoveConstDef> =
    moduleBlock?.constDefList.orEmpty()

fun MoveModuleDef.schemas(): List<MoveSchemaDef> =
    moduleBlock?.itemSpecDefList.orEmpty().mapNotNull { it.schemaDef }
