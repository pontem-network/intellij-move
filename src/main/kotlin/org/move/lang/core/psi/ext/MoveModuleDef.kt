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

//fun MoveModuleDef.importAliases(): List<MoveImportAlias> {
//
//    val items = mutableMapOf<Address, MoveImport>()
//    for (stmt in importStatements) {
//        val address = stmt.addressRef?.addressLiteral?.text ?: continue
//        items[Address(address)] = stmt.import ?: continue
//    }
//    return items
//}
////            else -> continue
////        }
////    }
//    importStatements.map {
//        when (it.import) {
//            is MoveModuleImport -> it.import
//            is MoveItemImport -> {
//            }
//            else -> null
//        }
//    }
//    importStatements.map { it.itemImport }
//    val memberImports = importStatements.mapNotNull { it.moduleItemsImport }
//    val imports = mutableMapOf<Address, List<MoveImport>>()
//    for (memberImport in memberImports) {
//        imports[memberImport.address()] = memberImport.importList
//    }
//    return imports
//}
//
//fun MoveModuleDef.imports(): Map<Address, List<MoveImport>> {
//    val memberImports = importStatements.mapNotNull { it.moduleMemberImport }
//    val imports = mutableMapOf<Address, List<MoveImport>>()
//    for (memberImport in memberImports) {
//        imports[memberImport.address()] = memberImport.importList
//    }
//    return imports
//}
//private fun MoveModuleDef.importsInner(): List<MoveImport> {
//    val importStatements = this.moduleBlock?.importStatementList.orEmpty()
//    val imports = mutableListOf<MoveImport>()
//    for (stmt in importStatements) {
//        imports.addIfNotNull(stmt.moduleImport?.import)
//        imports.addAll(stmt.moduleMemberImport?.importList.orEmpty())
//    }
//    return imports
//}

//fun MoveModuleDef.imports(): List<MoveImport> = importsInner().filter { it.importAlias == null }
//
//fun MoveModuleDef.moduleImports(): List<MoveModuleImport>
//
//fun MoveModuleDef.importAliases(): List<MoveImportAlias> = importsInner().mapNotNull { it.importAlias }

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
//
//fun MoveModuleDef.defs(): List<MoveNamedElement> {
//    val block = this.moduleBlock ?: return emptyList()
//    return listOf(
//        block.functionDefList,
////        block.nativeDefineFunctionList,
//        block.constDefList,
//        block.structDefList,
//        block.schemaDefList,
//    ).flatten()
////    val functions = block.functionDefList
////    val functions = PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveFunctionDef::class.java)
////    val nativeFunctions =
////        PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveNativeFunctionDef::class.java)
////    val structs = PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveStructDef::class.java)
////    val schemas = PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveSchemaDef::class.java)
////    val consts = PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveConstDef::class.java)
////    return listOf(
////        functions, nativeFunctions, structs, schemas, consts
////    ).flatten()
//}