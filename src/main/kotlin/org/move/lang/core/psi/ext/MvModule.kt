package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.ide.MoveIcons
import org.move.lang.core.completion.getOriginalOrSelf
import org.move.lang.core.psi.*
import org.move.lang.core.stubs.MvFunctionStub
import org.move.lang.core.stubs.MvModuleStub
import org.move.lang.core.stubs.MvStructStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.stubs.ext.childrenStubsOfType
import org.move.lang.core.types.Address
import org.move.lang.core.types.ItemQualName
import org.move.lang.core.types.address
import org.move.lang.index.MvModuleSpecIndex
import org.move.lang.moveProject
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.globalPsiDependentCache
import org.move.utils.psiCacheResult
import javax.swing.Icon

fun MvModule.hasTestFunctions(): Boolean = this.testFunctions().isNotEmpty()

fun MvModule.addressRef(): MvAddressRef? =
    this.addressRef ?: (this.ancestorStrict<MvAddressDef>())?.addressRef

val MvModule.friendModules: Sequence<MvModule>
    get() {
        return this.friendDeclList
            .asSequence()
            .mapNotNull { it.path.reference?.resolveFollowingAliases() as? MvModule }
    }

fun MvModule.allFunctions(): List<MvFunction> {
    val stub = greenStub
    return stub?.childrenStubsOfType<MvFunctionStub>()?.map { it.psi } ?: functionList
}

fun MvModule.allNonTestFunctions(): List<MvFunction> = this.allFunctions().filter { f -> !f.hasTestAttr }
fun MvModule.testFunctions(): List<MvFunction> = this.allFunctions().filter { f -> f.hasTestAttr }

val MvModule.isBuiltins: Boolean get() = this.name == "builtins" && (this.address(null)?.is0x0 ?: false)
val MvModule.isSpecBuiltins: Boolean
    get() = this.name == "spec_builtins" && (this.address(null)?.is0x0 ?: false)

// this is extremely fast, no need to optimize anymore
fun MvModule.builtinFunctions(): List<MvFunction> {
    return getProjectPsiDependentCache(this) {
        createBuiltinFunctions(it.project)
    }
}

private fun createBuiltinFunctions(project: Project): List<MvFunction> {
    val builtinModule = project.psiFactory.module(
        """
            module 0x0::builtins {
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
            }            
        """.trimIndent()
    )
    val builtinFunctions = builtinModule.functionList
    builtinFunctions.forEach { f -> (f as MvFunctionMixin).builtIn = true }
    return builtinFunctions
}

fun MvModule.entryFunctions(): List<MvFunction> = this.allFunctions().filter { it.isEntry }

fun MvModule.viewFunctions(): List<MvFunction> = this.allFunctions().filter { it.isView }

fun MvModule.specInlineFunctions(): List<MvSpecInlineFunction> =
    this.moduleItemSpecList.flatMap { it.specInlineFunctions() }

fun builtinSpecFunction(text: String, project: Project): MvSpecFunction {
    val trimmedText = text.trimIndent()
    return project.psiFactory.specFunction(trimmedText, moduleName = "builtin_spec_functions")
}

fun MvModule.tupleStructs(): List<MvStruct> =
    this.structs().filter { it.tupleFields != null }

fun MvModule.structs(): List<MvStruct> {
    return getProjectPsiDependentCache(this) {
        val stub = it.greenStub
        stub?.childrenStubsOfType<MvStructStub>()?.map { s -> s.psi } ?: it.structList
    }
}

//private val BUILTIN_SPEC_FUNCTIONS_KEY =
//    Key.create<CachedValue<List<MvSpecFunction>>>("org.move.BUILTIN_SPEC_FUNCTIONS_KEY")

fun MvModule.builtinSpecFunctions(): List<MvSpecFunction> {
    return getProjectPsiDependentCache(this) {
        val builtinsModule = it.project.psiFactory.module(
            """
            module 0x0::builtin_spec_functions {
                spec native fun max_u8(): num;
                spec native fun max_u64(): num;
                spec native fun max_u128(): num;
                spec native fun global<T: key>(addr: address): T;
                spec native fun old<T>(_: T): T;
                spec native fun update_field<S, F, V>(s: S, fname: F, val: V): S;
                spec native fun TRACE<T>(_: T): T;
                
                spec native fun concat<T>(v1: vector<T>, v2: vector<T>): vector<T>;
                spec native fun vec<T>(_: T): vector<T>;
                spec native fun len<T>(_: vector<T>): num;
                spec native fun contains<T>(v: vector<T>, e: T): bool;
                spec native fun index_of<T>(_: vector<T>, _: T): num;
                spec native fun range<T>(_: vector<T>): range;
                spec native fun update<T>(_: vector<T>, _: num, _: T): vector<T>;
                spec native fun in_range<T>(_: vector<T>, _: num): bool;
                spec native fun int2bv(_: num): bv;
                spec native fun bv2int(_: bv): num;
            }            
        """.trimIndent()
        )
        builtinsModule.specFunctionList
    }
}

fun MvModule.specFunctions(): List<MvSpecFunction> = specFunctionList.orEmpty()

fun MvModule.consts(): List<MvConst> = this.constList

fun MvModule.enumVariants(): List<MvEnumVariant> = this.enumList.flatMap { it.variants }

//fun MvModuleBlock.moduleItemSpecs() = this.moduleItemSpecList
////    this.childrenOfType<MvItemSpec>()
////        .filter { it.itemSpecRef?.moduleKw != null }

val MvModuleSpec.moduleItem: MvModule? get() = this.path?.reference?.resolve() as? MvModule

val MvModuleSpecBlock.moduleSpec: MvModuleSpec get() = this.parent as MvModuleSpec

fun MvModuleSpec.moduleItemSpecs(): List<MvModuleItemSpec> =
    this.moduleSpecBlock?.moduleItemSpecList.orEmpty()

fun MvModuleSpec.schemas(): List<MvSchema> = this.moduleSpecBlock?.schemaList.orEmpty()

fun MvModuleSpec.specFunctions(): List<MvSpecFunction> = this.moduleSpecBlock?.specFunctionList.orEmpty()

fun MvModuleSpec.specInlineFunctions(): List<MvSpecInlineFunction> =
    this.moduleItemSpecs().flatMap { it.specInlineFunctions() }

private val MODULE_SPECS_KEY: Key<CachedValue<List<MvModuleSpec>>> =
    Key.create("ALL_MODULE_SPECS_KEY")

fun MvModule.allModuleSpecs(): List<MvModuleSpec> = project.cacheManager.cache(this, MODULE_SPECS_KEY) {
    val specs: List<MvModuleSpec> = run {
        val moveProject = this.moveProject ?: return@run emptyList()
        val moduleName = this.name ?: return@run emptyList()

        val searchScope = moveProject.searchScope()
        // all `spec 0x1::m {}` for the current module
        val allModuleSpecs = MvModuleSpecIndex.getElementsByModuleName(this.project, moduleName, searchScope)
        if (allModuleSpecs.isEmpty()) return@run emptyList()

        allModuleSpecs
            .filter { moduleSpec ->
                val specModule = moduleSpec.moduleItem ?: return@filter false
                isModulesEqual(this, specModule)
            }
            .toList()
    }
    this.psiCacheResult(specs)
}

fun MvModule.allModuleSpecBlocks(): List<MvModuleSpecBlock> {
    return this.allModuleSpecs().mapNotNull { it.moduleSpecBlock }
}

fun isModulesEqual(left: MvModule, right: MvModule): Boolean {
    return left.getOriginalOrSelf() == right.getOriginalOrSelf()
}

abstract class MvModuleMixin: MvStubbedNamedElementImpl<MvModuleStub>,
                              MvModule {

    constructor(node: ASTNode): super(node)

    constructor(stub: MvModuleStub, nodeType: IStubElementType<*, *>): super(stub, nodeType)

    override fun getIcon(flags: Int): Icon = MoveIcons.MODULE

    override val qualName: ItemQualName?
        get() {
            // from stub
            val moduleName = this.name ?: return null
            val moveProject = this.moveProject
            // from stub
            val address = this.address(moveProject) ?: Address.Value("0x0")
            return ItemQualName(this, address, null, moduleName)
        }
}
