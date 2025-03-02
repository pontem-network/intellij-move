package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import org.move.ide.MoveIcons
import org.move.lang.core.completion.getOriginalOrSelf
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.resolve.PsiCachedValueProvider
import org.move.lang.core.resolve.getResults
import org.move.lang.core.types.address
import org.move.lang.index.MvModuleSpecFileIndex
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

fun MvModule.allNonTestFunctions(): List<MvFunction> = this.functionList.filter { f -> !f.hasTestAttr }
fun MvModule.testFunctions(): List<MvFunction> = this.functionList.filter { f -> f.hasTestAttr }

val MvModule.isBuiltins: Boolean get() = this.name == "builtins" && (this.address(null)?.is0x0 ?: false)
val MvModule.isSpecBuiltins: Boolean
    get() = this.name == "spec_builtins" && (this.address(null)?.is0x0 ?: false)

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

fun MvModule.entryFunctions(): List<MvFunction> = this.functionList.filter { it.isEntry }

fun MvModule.viewFunctions(): List<MvFunction> = this.functionList.filter { it.isView }

fun MvModule.tupleStructs(): List<MvStruct> =
    this.structList.filter { it.tupleFields != null }

fun MvModule.enumVariants(): List<MvEnumVariant> = this.enumList.flatMap { it.variants }

val MvModuleSpec.moduleItem: MvModule? get() = this.path?.reference?.resolve() as? MvModule

val MvModuleSpecBlock.moduleSpec: MvModuleSpec get() = this.parent as MvModuleSpec

fun MvModuleSpec.moduleItemSpecs(): List<MvModuleItemSpec> =
    this.moduleSpecBlock?.moduleItemSpecList.orEmpty()

fun MvModuleSpec.schemas(): List<MvSchema> = this.moduleSpecBlock?.schemaList.orEmpty()

fun MvModuleSpec.specFunctions(): List<MvSpecFunction> = this.moduleSpecBlock?.specFunctionList.orEmpty()

class ModuleSpecsFromIndex(override val owner: MvModule): PsiCachedValueProvider<List<MvModuleSpec>> {
    override fun compute(): CachedValueProvider.Result<List<MvModuleSpec>> {
        val specs = MvModuleSpecFileIndex.getSpecsForModule(owner)
        return owner.psiCacheResult(specs)
    }
}

fun MvModule.getModuleSpecsFromIndex(): List<MvModuleSpec> = ModuleSpecsFromIndex(this).getResults()

fun isModulesEqual(left: MvModule, right: MvModule): Boolean {
    return left.getOriginalOrSelf() == right.getOriginalOrSelf()
}

abstract class MvModuleMixin(node: ASTNode): MvNameIdentifierOwnerImpl(node),
                                             MvModule {

    override fun getIcon(flags: Int): Icon = MoveIcons.MODULE
}
