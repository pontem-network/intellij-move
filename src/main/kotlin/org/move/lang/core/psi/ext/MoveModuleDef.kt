package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import org.move.ide.MoveIcons
import org.move.lang.containingMoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.psi.mixins.MoveFunctionSignatureMixin
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.FQModule
import org.move.lang.moveProject
import javax.swing.Icon

fun MoveModuleDef.definedAddressRef(): MoveAddressRef? =
    this.addressRef ?: (this.ancestorStrict<MoveAddressDef>())?.addressRef

fun MoveModuleDef.fqModule(): FQModule? {
//    val moveProject = this.containingFile.containingMoveProject() ?: return null
    val address = this.containingAddress.normalized()
    val name = this.name ?: return null
    return FQModule(address, name)
}

val MoveModuleDef.fqName: String? get() {
    val address = this.addressRef?.text?.let { "$it::" } ?: ""
    val module = this.name ?: return null
    return address + module
}

val MoveModuleDef.friendModules: Set<FQModule>
    get() {
        val block = this.moduleBlock ?: return emptySet()
        val moduleRefs = block.friendStatementList.mapNotNull { it.fqModuleRef }

        val friends = mutableSetOf<FQModule>()
//        val moveProject = this.moveProject() ?: return emptySet()
        for (moduleRef in moduleRefs) {
            val address = moduleRef.addressRef.toNormalizedAddress() ?: continue
            val identifier = moduleRef.identifier?.text ?: continue
            friends.add(FQModule(address, identifier))
        }
        return friends
    }

fun MoveModuleDef.allFnSignatures(): List<MoveFunctionSignature> {
    val block = moduleBlock ?: return emptyList()
    return listOf(
        block.functionDefList.mapNotNull { it.functionSignature },
        block.nativeFunctionDefList.mapNotNull { it.functionSignature },
    ).flatten()
}

fun MoveModuleDef.builtinFnSignatures(): List<MoveFunctionSignature> {
    return listOfNotNull(
        createBuiltinFuncSignature("native fun move_from<R: key>(addr: address): R;", project),
        createBuiltinFuncSignature("native fun move_to<R: key>(acc: &signer, res: R);", project),
        createBuiltinFuncSignature("native fun borrow_global<R: key>(addr: address): &R;", project),
        createBuiltinFuncSignature(
            "native fun borrow_global_mut<R: key>(addr: address): &mut R;",
            project
        ),
        createBuiltinFuncSignature("native fun exists<R: key>(addr: address): bool;", project),
        createBuiltinFuncSignature("native fun freeze<S>(mut_ref: &mut S): &S;", project),
        createBuiltinFuncSignature("native fun assert(_: bool, err: u64);", project),
    )
}

fun MoveModuleDef.functionSignatures(visibility: Visibility): List<MoveFunctionSignature> =
    when (visibility) {
        is Visibility.Public ->
            allFnSignatures()
                .filter { it.visibility == FunctionVisibility.PUBLIC }
        is Visibility.PublicScript ->
            allFnSignatures()
                .filter { it.visibility == FunctionVisibility.PUBLIC_SCRIPT }
        is Visibility.PublicFriend -> {
            if (visibility.currentModule in this.friendModules) {
                allFnSignatures().filter { it.visibility == FunctionVisibility.PUBLIC_FRIEND }
            } else {
                emptyList()
            }
        }
        is Visibility.Internal -> allFnSignatures()
    }

//fun MoveModuleDef.publicFnSignatures(): List<MoveFunctionSignature> {
//    return allFnSignatures()
//        .filter { it.visibility == FunctionVisibility.PUBLIC }
//}

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

//fun MoveModuleDef.schemas(): List<MoveSchemaDef> =
//    moduleBlock?.itemSpecDefList.orEmpty().mapNotNull { it.schemaDef }


abstract class MoveModuleDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                   MoveModuleDef {
//    constructor(node: ASTNode) : super(node)
//
//    constructor(stub: MoveModuleDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getIcon(flags: Int): Icon = MoveIcons.MODULE

    override fun getPresentation(): ItemPresentation? {
        val name = this.name ?: return null
//        val moveProject = this.containingFile.containingMoveProject() ?: return null
        val locationString = this.containingAddress.text
        return PresentationData(name,
                                locationString,
                                MoveIcons.MODULE,
                                null)
    }

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
