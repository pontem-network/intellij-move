package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import org.move.ide.MvIcons
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.psi.mixins.MvFunctionSignatureMixin
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.FQModule
import javax.swing.Icon

fun List<MvAttr>.findSingleItemAttr(name: String): MvAttr? =
    this.find {
        it.attrItemList.size == 1
                && it.attrItemList.first().identifier.text == name
    }

val MvModuleDef.isTestOnly: Boolean get() = this.attrList.findSingleItemAttr("test_only") != null

fun MvModuleDef.definedAddressRef(): MvAddressRef? =
    this.addressRef ?: (this.ancestorStrict<MvAddressDef>())?.addressRef

fun MvModuleDef.fqModule(): FQModule? {
//    val moveProject = this.containingFile.containingMvProject() ?: return null
    val address = this.containingAddress.normalized()
    val name = this.name ?: return null
    return FQModule(address, name)
}

val MvModuleDef.fqName: String?
    get() {
        val address = this.definedAddressRef()?.text?.let { "$it::" } ?: ""
        val module = this.name ?: return null
        return address + module
    }

val MvModuleDef.friendModules: Set<FQModule>
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

fun MvModuleDef.allFnSignatures(): List<MvFunctionSignature> {
    val block = moduleBlock ?: return emptyList()
    return listOf(
        block.functionDefList.mapNotNull { it.functionSignature },
        block.nativeFunctionDefList.mapNotNull { it.functionSignature },
    ).flatten()
}

fun MvModuleDef.builtinFnSignatures(): List<MvFunctionSignature> {
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

fun MvModuleDef.functionSignatures(visibility: Visibility): List<MvFunctionSignature> =
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

//fun MvModuleDef.publicFnSignatures(): List<MvFunctionSignature> {
//    return allFnSignatures()
//        .filter { it.visibility == FunctionVisibility.PUBLIC }
//}

fun createBuiltinFuncSignature(text: String, project: Project): MvFunctionSignature? {
    val signature = MvPsiFactory(project)
        .createNativeFunctionDef(text, moduleName = "builtin_functions")
        .functionSignature ?: return null
    (signature as MvFunctionSignatureMixin).builtIn = true
    return signature
}

//fun MvModuleDef.builtinFunctions(): List<MvNativeFunctionDef> =
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

//fun MvModuleDef.nativeFunctions(): List<MvNativeFunctionDef> =
//    emptyList()
//    moduleBlock?.nativeFunctionDefList.orEmpty()

//fun MvModuleDef.publicNativeFunctions(): List<MvNativeFunctionDef> =
//    nativeFunctions().filter { it.isPublic }

//fun MvModuleDef.structs(): List<MvStructDef> =
//    moduleBlock?.structDefList.orEmpty()

fun MvModuleDef.structSignatures(): List<MvStructSignature> {
    val block = moduleBlock ?: return emptyList()
    return listOf(
        block.nativeStructDefList.mapNotNull { it.structSignature },
        block.structDefList.map { it.structSignature }
    ).flatten()
}

//fun MvModuleDef.nativeStructs(): List<MvStructSignature> {
////    val block = moduleBlock ?: return emptyList()
//    return emptyList()
//}

fun MvModuleDef.constBindings(): List<MvBindingPat> =
    moduleBlock?.constDefList.orEmpty().mapNotNull { it.bindingPat }

//fun MvModuleDef.publicConsts(): List<MvConstDef> =
//    consts().filter {  }

//fun MvModuleDef.schemas(): List<MvSchemaDef> =
//    moduleBlock?.itemSpecDefList.orEmpty().mapNotNull { it.schemaDef }


abstract class MvModuleDefMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                 MvModuleDef {
//    constructor(node: ASTNode) : super(node)
//
//    constructor(stub: MvModuleDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getIcon(flags: Int): Icon = MvIcons.MODULE

    override fun getPresentation(): ItemPresentation? {
        val name = this.name ?: return null
//        val moveProject = this.containingFile.containingMvProject() ?: return null
        val locationString = this.containingAddress.text
        return PresentationData(
            name,
            locationString,
            MvIcons.MODULE,
            null
        )
    }

    override val importStatements: List<MvImportStatement>
        get() =
            moduleBlock?.importStatementList.orEmpty()
}

//abstract class MvModuleDefMixin : MvStubbedNameIdentifierOwnerImpl<MvModuleDefStub>,
//                                    MvModuleDef {
//    constructor(node: ASTNode) : super(node)
//
//    constructor(stub: MvModuleDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
//
//    override val importStatements: List<MvImportStatement>
//        get() =
//            moduleBlock?.importStatementList.orEmpty()
//}

//abstract class MvModuleDefMixin : MvStubbedNameIdentifierOwnerImpl<MvModuleDefStub>,
//                                    MvModuleDef {
//    constructor(node: ASTNode) : super(node)
//
//    constructor(stub: MvModuleDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
//
//    override val importStatements: List<MvImportStatement>
//        get() =
//            moduleBlock?.importStatementList.orEmpty()
//}
