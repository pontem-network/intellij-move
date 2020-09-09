package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

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