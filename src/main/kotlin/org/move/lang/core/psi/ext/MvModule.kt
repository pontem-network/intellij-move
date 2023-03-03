package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.ide.MoveIcons
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.stubs.MvFunctionStub
import org.move.lang.core.stubs.MvModuleStub
import org.move.lang.core.stubs.MvStructStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.stubs.ext.childrenStubsOfType
import org.move.lang.core.types.Address
import org.move.lang.core.types.address
import org.move.lang.index.MvModuleSpecIndex
import org.move.lang.moveProject
import javax.swing.Icon

fun MvModule.hasTestFunctions(): Boolean = this.testFunctions().isNotEmpty()

fun MvModule.addressRef(): MvAddressRef? =
    this.addressRef ?: (this.ancestorStrict<MvAddressDef>())?.addressRef

data class FQModule(val address: Address, val name: String)

fun MvModule.fqModule(): FQModule? {
    val moveProj = this.moveProject ?: return null
    val address = this.address(moveProj) ?: return null
    val name = this.name ?: return null
    return FQModule(address, name)
}

val MvModule.declaredFriendModules: Set<FQModule>
    get() {
        val block = this.moduleBlock ?: return emptySet()
        val friendModuleRefs = block.friendDeclList.mapNotNull { it.fqModuleRef }
        val moveProj = block.moveProject

        val friends = mutableSetOf<FQModule>()
        for (moduleRef in friendModuleRefs) {
            val address = moduleRef.addressRef.address(moveProj) ?: continue
            val identifier = moduleRef.identifier?.text ?: continue
            friends.add(FQModule(address, identifier))
        }
        return friends
    }

fun MvModule.allFunctions(): List<MvFunction> {
    val stub = greenStub
    return stub?.childrenStubsOfType<MvFunctionStub>()?.map { it.psi }
        ?: moduleBlock?.functionList.orEmpty()
}

fun MvModule.allNonTestFunctions(): List<MvFunction> =
//    allFunctions().filter { f -> !f.isTest }
    getProjectPsiDependentCache(this) {
        it.allFunctions().filter { f -> !f.isTest }
    }

fun MvModule.testFunctions(): List<MvFunction> =
//    allFunctions().filter { f -> f.isTest }
    getProjectPsiDependentCache(this) {
        it.allFunctions().filter { f -> f.isTest }
    }

fun MvModule.builtinFunctions(): List<MvFunction> {
    return getProjectPsiDependentCache(this) {
        val text = """
            /// Removes `T` from address and returns it. 
            /// Aborts if address does not hold a `T`.
            native fun move_from<T: key>(addr: address): T acquires T;
                        
            /// Publishes `T` under `signer.address`. 
            /// Aborts if `signer.address` already holds a `T`.
            native fun move_to<T: key>(acc: &signer, res: T);
                                    
            native fun borrow_global<T: key>(addr: address): &T acquires T;           
                                     
            native fun borrow_global_mut<T: key>(addr: address): &mut T acquires T;
            
            /// Returns `true` if a `T` is stored under address
            native fun exists<T: key>(addr: address): bool;
            
            native fun freeze<S>(mut_ref: &mut S): &S;
        """.trimIndent()
        val builtinFunctions = it.project.psiFactory.functions(text, moduleName = "builtins")
        builtinFunctions.forEach { f -> (f as MvFunctionMixin).builtIn = true }
        builtinFunctions
    }
}

fun MvModule.visibleFunctions(visibility: Visibility): List<MvFunction> {
    return when (visibility) {
        is Visibility.Public ->
            allNonTestFunctions()
                .filter { it.visibility == FunctionVisibility.PUBLIC }
        is Visibility.PublicScript ->
            allNonTestFunctions()
                .filter { it.visibility == FunctionVisibility.PUBLIC_SCRIPT }
        is Visibility.PublicFriend -> {
            val friendFunctions =
                allNonTestFunctions().filter { it.visibility == FunctionVisibility.PUBLIC_FRIEND }
            if (friendFunctions.isEmpty()) return emptyList()

            val currentModule = visibility.currentModule.element
            if (currentModule != null
                && currentModule.fqModule() in this.declaredFriendModules
            ) {
                friendFunctions
            } else {
                emptyList()
            }
        }
        is Visibility.Internal -> allNonTestFunctions()
    }
}

fun MvModule.entryFunctions(): List<MvFunction> = this.allFunctions().filter { it.isEntry }

fun MvModule.viewFunctions(): List<MvFunction> = this.allFunctions().filter { it.isView }

fun builtinFunction(text: String, project: Project): MvFunction {
    val trimmedText = text.trimIndent()
    val function = project.psiFactory.function(trimmedText, moduleName = "builtins")
    (function as MvFunctionMixin).builtIn = true
    return function
}

fun builtinSpecFunction(text: String, project: Project): MvSpecFunction {
    val trimmedText = text.trimIndent()
    return project.psiFactory.specFunction(trimmedText, moduleName = "builtin_spec_functions")
}

fun MvModule.structs(): List<MvStruct> {
    return getProjectPsiDependentCache(this) {
        val stub = it.greenStub
        stub?.childrenStubsOfType<MvStructStub>()?.map { s -> s.psi }
            ?: it.moduleBlock?.structList.orEmpty()
    }
}

fun MvModule.schemas(): List<MvSchema> = moduleBlock?.schemaList.orEmpty()

fun MvModule.builtinSpecFunctions(): List<MvSpecFunction> {
    return getProjectPsiDependentCache(this) {
        val project = it.project
        listOf(
            builtinSpecFunction("spec native fun max_u8(): num;", project),
            builtinSpecFunction("spec native fun max_u64(): num;", project),
            builtinSpecFunction("spec native fun max_u128(): num;", project),
            builtinSpecFunction("spec native fun global<T: key>(addr: address): T;", project),
            builtinSpecFunction("spec native fun old<T>(_: T): T;", project),
            builtinSpecFunction(
                "spec native fun update_field<S, F, V>(s: S, fname: F, val: V): S;",
                project
            ),
            builtinSpecFunction("spec native fun TRACE<T>(_: T): T;", project),
            // vector functions
            builtinSpecFunction("spec native fun len<T>(_: vector<T>): num;", project),
            builtinSpecFunction(
                "spec native fun concat<T>(v1: vector<T>, v2: vector<T>): vector<T>;",
                project
            ),
            builtinSpecFunction("spec native fun contains<T>(v: vector<T>, e: T): bool;", project),
            builtinSpecFunction("spec native fun index_of<T>(_: vector<T>, _: T): num;", project),
            builtinSpecFunction("spec native fun range<T>(_: vector<T>): range;", project),
            builtinSpecFunction("spec native fun in_range<T>(_: vector<T>, _: num): bool;", project),
        )
    }
}

fun MvModule.specFunctions(): List<MvSpecFunction> = moduleBlock?.specFunctionList.orEmpty()

fun MvModule.consts(): List<MvConst> =
    getProjectPsiDependentCache(this) {
        it.moduleBlock?.constList.orEmpty()
    }
//fun MvModule.constBindings(): List<MvBindingPat> =
//    getProjectPsiDependentCache(this) {
//        it.moduleBlock?.constList.orEmpty().mapNotNull { c -> c.bindingPat }
//    }

val MvModuleBlock.module: MvModule get() = this.parent as MvModule

fun MvModuleBlock.moduleItemSpecs() = this.moduleItemSpecList
//    this.childrenOfType<MvItemSpec>()
//        .filter { it.itemSpecRef?.moduleKw != null }

val MvModuleSpec.module: MvModule? get() = this.fqModuleRef?.reference?.resolve() as? MvModule

val MvModuleSpecBlock.moduleSpec: MvModuleSpec get() = this.parent as MvModuleSpec

fun MvModuleBlock.itemSpecs() = this.childrenOfType<MvItemSpec>()

fun MvModuleSpecBlock.itemSpecs() = this.childrenOfType<MvItemSpec>()

//fun MvModuleSpecBlock.moduleItemSpecs() =
//    this.itemSpecs()
//        .filter { it.itemSpecRef?.moduleKw != null }

fun MvModule.allModuleSpecs(): List<MvModuleSpec> {
    return getProjectPsiDependentCache(this) {
        val moveProject = it.moveProject ?: return@getProjectPsiDependentCache emptyList()
        val moduleName = it.name ?: return@getProjectPsiDependentCache emptyList()
        val file = it.containingMoveFile ?: return@getProjectPsiDependentCache emptyList()

        val searchScope = moveProject.searchScope()
        val moduleSpecs = file.moduleSpecs() +
                MvModuleSpecIndex.getElementsByModuleName(it.project, moduleName, searchScope)
        if (moduleSpecs.isEmpty()) return@getProjectPsiDependentCache emptyList()

        val currentModule = it.fqModule() ?: return@getProjectPsiDependentCache emptyList()
        moduleSpecs
            .filter { moduleSpec ->
                val module = moduleSpec.fqModuleRef?.reference?.resolve() as? MvModule ?: return@filter false
                currentModule == module.fqModule()
            }
            .toList()
    }
}

fun MvModule.allModuleSpecBlocks(): List<MvModuleSpecBlock> {
    return this.allModuleSpecs().mapNotNull { it.moduleSpecBlock }
}

abstract class MvModuleMixin : MvStubbedNamedElementImpl<MvModuleStub>,
                               MvModule {

    constructor(node: ASTNode) : super(node)

    constructor(stub: MvModuleStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getIcon(flags: Int): Icon = MoveIcons.MODULE

    override fun getPresentation(): ItemPresentation? {
        val name = this.name ?: return null
        val moveProj = this.moveProject
        val locationString = this.address(moveProj)?.text() ?: ""
        return PresentationData(
            name,
            locationString,
            MoveIcons.MODULE,
            null
        )
    }

    override val fqName: String
        get() {
            val address = this.addressRef()?.text?.let { "$it::" } ?: ""
            val module = this.name ?: "<unknown>"
            return address + module
        }
}
