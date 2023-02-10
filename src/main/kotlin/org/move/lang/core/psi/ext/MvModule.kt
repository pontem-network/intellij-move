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
import org.move.lang.core.stubs.MvModuleStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.FQModule
import org.move.lang.index.MvModuleSpecIndex
import org.move.lang.moveProject
import javax.swing.Icon

fun MvModule.hasTestFunctions(): Boolean = this.testFunctions().isNotEmpty()

fun MvModule.address(): MvAddressRef? =
    this.addressRef ?: (this.ancestorStrict<MvAddressDef>())?.addressRef

fun MvModule.stubText(): String {
    val address = this.moveProject
        ?.let { this.addressRef?.serializedAddressText(it) } ?: "<unknown>"
    return "$address::${this.name}"
}

fun MvModule.fqModule(): FQModule? {
    return getProjectPsiDependentCache(this) {
        val address = this.address()?.toAddress() ?: return@getProjectPsiDependentCache null
        val name = this.name ?: return@getProjectPsiDependentCache null
        FQModule(address, name)
    }
}

val MvModule.friendModules: Set<FQModule>
    get() {
        val block = this.moduleBlock ?: return emptySet()
        val moduleRefs = block.friendDeclList.mapNotNull { it.fqModuleRef }

        val friends = mutableSetOf<FQModule>()
        for (moduleRef in moduleRefs) {
            val proj = moduleRef.moveProject ?: continue
            val address = moduleRef.addressRef.toAddress(proj) ?: continue
            val identifier = moduleRef.identifier?.text ?: continue
            friends.add(FQModule(address, identifier))
        }
        return friends
    }

fun MvModule.allFunctions(): List<MvFunction> = moduleBlock?.functionList.orEmpty()

fun MvModule.allNonTestFunctions(): List<MvFunction> =
    getProjectPsiDependentCache(this) {
        it.allFunctions().filter { f -> !f.isTest }
    }

fun MvModule.testFunctions(): List<MvFunction> =
    getProjectPsiDependentCache(this) {
        it.allFunctions().filter { f -> f.isTest }
    }

fun MvModule.builtinFunctions(): List<MvFunction> {
    return getProjectPsiDependentCache(this) {
        listOf(
            builtinFunction(
                """
            /// Removes `T` from address and returns it. 
            /// Aborts if address does not hold a `T`.
            native fun move_from<T: key>(addr: address): T acquires T;
            """, it.project
            ),
            builtinFunction(
                """
            /// Publishes `T` under `signer.address`. 
            /// Aborts if `signer.address` already holds a `T`.
            native fun move_to<T: key>(acc: &signer, res: T);
            """, it.project
            ),
            builtinFunction("native fun borrow_global<T: key>(addr: address): &T acquires T;", it.project),
            builtinFunction(
                "native fun borrow_global_mut<T: key>(addr: address): &mut T acquires T;",
                it.project
            ),
            builtinFunction(
                """
            /// Returns `true` if a `T` is stored under address
            native fun exists<T: key>(addr: address): bool;
            """, it.project
            ),
            builtinFunction("native fun freeze<S>(mut_ref: &mut S): &S;", it.project),
        )
    }
}

fun MvModule.visibleFunctions(visibility: Visibility): List<MvFunction> =
    when (visibility) {
        is Visibility.Public ->
            allNonTestFunctions()
                .filter { it.visibility == FunctionVisibility.PUBLIC }
        is Visibility.PublicScript ->
            allNonTestFunctions()
                .filter { it.visibility == FunctionVisibility.PUBLIC_SCRIPT }
        is Visibility.PublicFriend -> {
            if (visibility.currentModule in this.friendModules) {
                allNonTestFunctions().filter { it.visibility == FunctionVisibility.PUBLIC_FRIEND }
            } else {
                emptyList()
            }
        }
        is Visibility.Internal -> allNonTestFunctions()
    }

fun MvModule.entryFunctions(): List<MvFunction> = this.allFunctions().filter { it.isEntry }

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

fun MvModule.structs(): List<MvStruct> = moduleBlock?.structList.orEmpty()

fun MvModule.schemas(): List<MvSchema> = moduleBlock?.schemaList.orEmpty()

fun MvModule.builtinSpecFunctions(): List<MvSpecFunction> {
    return getProjectPsiDependentCache(this) {
        listOf(
            builtinSpecFunction("spec native fun max_u8(): num;", it.project),
            builtinSpecFunction("spec native fun max_u64(): num;", it.project),
            builtinSpecFunction("spec native fun max_u128(): num;", it.project),
            builtinSpecFunction("spec native fun global<T: key>(addr: address): T;", it.project),
            builtinSpecFunction("spec native fun old<T>(_: T): T;", it.project),
            builtinSpecFunction(
                "spec native fun update_field<S, F, V>(s: S, fname: F, val: V): S;",
                it.project
            ),
            builtinSpecFunction("spec native fun TRACE<T>(_: T): T;", it.project),
            // vector functions
            builtinSpecFunction("spec native fun len<T>(_: vector<T>): num;", it.project),
            builtinSpecFunction(
                "spec native fun concat<T>(v1: vector<T>, v2: vector<T>): vector<T>;",
                it.project
            ),
            builtinSpecFunction("spec native fun contains<T>(v: vector<T>, e: T): bool;", it.project),
            builtinSpecFunction("spec native fun index_of<T>(_: vector<T>, _: T): num;", it.project),
            builtinSpecFunction("spec native fun range<T>(_: vector<T>): range;", it.project),
            builtinSpecFunction("spec native fun in_range<T>(_: vector<T>, _: num): bool;", it.project),
        )
    }
}

fun MvModule.specFunctions(): List<MvSpecFunction> = moduleBlock?.specFunctionList.orEmpty()

fun MvModule.consts(): List<MvConst> = moduleBlock?.constList.orEmpty()

fun MvModule.constBindings(): List<MvBindingPat> =
    moduleBlock?.constList.orEmpty().mapNotNull { it.bindingPat }

val MvModuleBlock.module: MvModule get() = this.parent as MvModule

fun MvModuleBlock.moduleItemSpecs() =
    this.childrenOfType<MvItemSpec>()
        .filter { it.itemSpecRef?.moduleKw != null }

val MvModuleSpec.module: MvModule? get() = this.fqModuleRef?.reference?.resolve() as? MvModule

val MvModuleSpecBlock.moduleSpec: MvModuleSpec get() = this.parent as MvModuleSpec

fun MvModuleBlock.itemSpecs() = this.childrenOfType<MvItemSpec>()

fun MvModuleSpecBlock.itemSpecs() = this.childrenOfType<MvItemSpec>()

fun MvModuleSpecBlock.moduleItemSpecs() =
    this.itemSpecs()
        .filter { it.itemSpecRef?.moduleKw != null }

fun MvModule.allModuleSpecs(): List<MvModuleSpec> {
    return getProjectPsiDependentCache(this) {
        val currentModule = it.fqModule() ?: return@getProjectPsiDependentCache emptyList()
        val moveProject = it.moveProject ?: return@getProjectPsiDependentCache emptyList()
        val specFiles =
            MvModuleSpecIndex.moduleSpecFiles(it.project, it, moveProject.searchScope())
        specFiles
            .flatMap { f -> f.moduleSpecs() }
            .filter { moduleSpec ->
                val module = moduleSpec.fqModuleRef?.reference?.resolve() as? MvModule ?: return@filter false
                currentModule == module.fqModule()
            }
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
        val locationString = this.address()?.toAddress()?.text() ?: ""
        return PresentationData(
            name,
            locationString,
            MoveIcons.MODULE,
            null
        )
    }

    override val fqName: String
        get() {
            val address = this.address()?.text?.let { "$it::" } ?: ""
            val module = this.name ?: "<unknown>"
            return address + module
        }
}
