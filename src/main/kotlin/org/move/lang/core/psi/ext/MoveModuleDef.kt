package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.psi.mixins.MoveFunctionSignatureMixin

fun MoveModuleDef.allFnSignatures(): List<MoveFunctionSignature> {
    val block = moduleBlock ?: return emptyList()
    return listOf(
        block.functionDefList.mapNotNull { it.functionSignature },
        block.nativeFunctionDefList.mapNotNull { it.functionSignature },
    ).flatten()
}

fun MoveModuleDef.builtinFnSignatures(): List<MoveFunctionSignature> {
    return listOfNotNull(
        createBuiltinFuncSignature("native fun move_from<R: resource>(addr: address): R;", project),
        createBuiltinFuncSignature("native fun move_to<R: resource>(addr: address, res: R): ();", project),
        createBuiltinFuncSignature("native fun borrow_global<R: resource>(addr: address): &R;", project),
        createBuiltinFuncSignature(
            "native fun borrow_global_mut<R: resource>(addr: address): &mut R;",
            project
        ),
        createBuiltinFuncSignature("native fun exists<R: resource>(addr: address): bool;", project),
        createBuiltinFuncSignature("native fun freeze<S>(mut_ref: &mut S): &S;", project),
        createBuiltinFuncSignature("native fun assert(_: bool, err: u64): ();", project),
    )
}

fun MoveModuleDef.publicFnSignatures(): List<MoveFunctionSignature> {
    return allFnSignatures()
        .filter { it.visibility == FunctionVisibility.PUBLIC }
}

fun createBuiltinFuncSignature(text: String, project: Project): MoveFunctionSignature? {
    val signature = MovePsiFactory(project)
        .createNativeFunctionDef(text)
        .functionSignature ?: return null
    (signature as MoveFunctionSignatureMixin).builtIn = true
    return signature
}

//fun MoveModuleDef.builtinFunctions(): List<MoveNativeFunctionDef> =
//    listOf(
//        createBuiltinFuncSignature("native fun move_from<R: resource>(addr: address): R;", project),
//        createBuiltinFuncSignature("native fun move_to<R: resource>(addr: address, res: R): ();", project),
//        createBuiltinFuncSignature("native fun borrow_global<R: resource>(addr: address): &R;", project),
//        createBuiltinFuncSignature(
//            "native fun borrow_global_mut<R: resource>(addr: address): &mut R;",
//            project
//        ),
//        createBuiltinFuncSignature("native fun exists<R: resource>(addr: address): bool;", project),
//        createBuiltinFuncSignature("native fun freeze<S>(mut_ref: &mut S): &S;", project),
//        createBuiltinFuncSignature("native fun assert(_: bool, err: u64): ();", project),
//    )

//fun MoveModuleDef.nativeFunctions(): List<MoveNativeFunctionDef> =
//    emptyList()
//    moduleBlock?.nativeFunctionDefList.orEmpty()

//fun MoveModuleDef.publicNativeFunctions(): List<MoveNativeFunctionDef> =
//    nativeFunctions().filter { it.isPublic }

//fun MoveModuleDef.structs(): List<MoveStructDef> =
//    moduleBlock?.structDefList.orEmpty()

fun MoveModuleDef.structSignatures(): List<MoveStructSignature> {
    val block = moduleBlock ?: return emptyList()
    return listOf(
        block.nativeStructDefList.mapNotNull { it.structSignature },
        block.structDefList.map { it.structSignature }
    ).flatten()
}

//fun MoveModuleDef.nativeStructs(): List<MoveStructSignature> {
////    val block = moduleBlock ?: return emptyList()
//    return emptyList()
//}

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
