package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.psi.mixins.MoveNativeFunctionDefMixin

fun MoveModuleDef.functions(): List<MoveFunctionDef> =
    moduleBlock?.functionDefList.orEmpty()

fun MoveModuleDef.publicFunctions(): List<MoveFunctionDef> =
    functions().filter { it.isPublic }

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

fun MoveModuleDef.publicNativeFunctions(): List<MoveNativeFunctionDef> =
    nativeFunctions().filter { it.isPublic }

fun MoveModuleDef.structs(): List<MoveStructDef> =
    moduleBlock?.structDefList.orEmpty()

fun MoveModuleDef.nativeStructs(): List<MoveNativeStructDef> =
    moduleBlock?.nativeStructDefList.orEmpty()

fun MoveModuleDef.consts(): List<MoveConstDef> =
    moduleBlock?.constDefList.orEmpty()

//fun MoveModuleDef.publicConsts(): List<MoveConstDef> =
//    consts().filter {  }

fun MoveModuleDef.schemas(): List<MoveSchemaDef> =
    moduleBlock?.itemSpecDefList.orEmpty().mapNotNull { it.schemaDef }


abstract class MoveModuleDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                   MoveModuleDef {
//    constructor(node: ASTNode) : super(node)
//
//    constructor(stub: MoveModuleDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override val importStatements: List<MoveImportStatement>
        get() =
            moduleBlock?.importStatementList.orEmpty()
}

//abstract class MoveModuleDefMixin : MoveStubbedNameIdentifierOwnerImpl<MoveModuleDefStub>,
//                                    MoveModuleDef {
//    constructor(node: ASTNode) : super(node)
//
//    constructor(stub: MoveModuleDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
//
//    override val importStatements: List<MoveImportStatement>
//        get() =
//            moduleBlock?.importStatementList.orEmpty()
//}

//abstract class MoveModuleDefMixin : MoveStubbedNameIdentifierOwnerImpl<MoveModuleDefStub>,
//                                    MoveModuleDef {
//    constructor(node: ASTNode) : super(node)
//
//    constructor(stub: MoveModuleDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
//
//    override val importStatements: List<MoveImportStatement>
//        get() =
//            moduleBlock?.importStatementList.orEmpty()
//}