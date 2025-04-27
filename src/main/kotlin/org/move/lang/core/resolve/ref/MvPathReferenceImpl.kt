package org.move.lang.core.resolve.ref

import com.intellij.psi.ResolveResult
import org.move.cli.MoveProject
import org.move.lang.core.completion.getOriginalOrSelf
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.PathKind.ValueAddress
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.resolve.scopeEntry.asEntries
import org.move.lang.core.resolve.scopeEntry.filterByName
import org.move.lang.core.resolve.scopeEntry.toPathResolveResults
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.moveProject
import kotlin.LazyThreadSafetyMode.NONE

interface InferenceCachedPathElement: MvElement {
    val path: MvPath
}

class MvPathReferenceImpl(element: MvPath): MvPolyVariantReferenceBase<MvPath>(element),
                                            MvPathReference {

    override fun resolve(): MvNamedElement? = rawMultiResolveIfVisible().singleOrNull()?.element

    override fun multiResolve(): List<MvNamedElement> = rawMultiResolveIfVisible().map { it.element }

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        rawMultiResolve().toTypedArray()

    fun rawMultiResolveIfVisible(): List<RsPathResolveResult> =
        rawMultiResolve().filter { it.isVisible }

    fun rawMultiResolve(): List<RsPathResolveResult> =
        rawMultiResolveUsingInferenceCache() ?: rawCachedMultiResolve()

    private fun rawMultiResolveUsingInferenceCache(): List<RsPathResolveResult>? {
        val pathElement = element.parent
        val msl = pathElement.isMsl()

        // try resolving MvPathType inside MvIsExpr
        if (pathElement is MvPathType) {
            if (pathElement.parent is MvIsExpr) {
                // special case for MvPathType, specifically for is expr
                return getResolvedPathFromInference(element, msl)
            }
            // other path type cases are not cached in the inference
            return null
        }
        if (pathElement is InferenceCachedPathElement) {
            return getResolvedPathFromInference(pathElement.path, msl)
        }
        return null
    }

    private fun getResolvedPathFromInference(path: MvPath, msl: Boolean): List<RsPathResolveResult>? {
        // Path resolution is cached, but sometimes path changes so much that it can't be retrieved
        // from cache anymore. In this case we need to get the old path.
        val originalPath = path.getOriginalOrSelf()
        return originalPath.inference(msl)?.getResolvedPath(originalPath)
            ?.map {
                RsPathResolveResult(it.element, it.isVisible)
            }
    }

    private fun rawCachedMultiResolve(): List<RsPathResolveResult> {
        return MvResolveCache
            .getInstance(element.project)
            .resolveWithCaching(element, ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE, Resolver)
            .orEmpty()
    }

    private object Resolver: (MvElement) -> List<RsPathResolveResult> {
        override fun invoke(path: MvElement): List<RsPathResolveResult> {
            // should not really happen
            if (path !is MvPath) return emptyList()

//            val ctx = ResolutionContext(path, isCompletion = false)
            val entries = resolvePath(path, expectedType = null)
            return entries.toPathResolveResults(contextElement = path)
        }
    }
}

fun getPathResolveVariantsWithExpectedType(
    ctx: ResolutionContext,
    pathKind: PathKind,
    expectedType: Ty?,
): List<ScopeEntry> {
    // if path qualifier is enum, then the expected type is that enum
    var correctedExpectedType = expectedType
    if (
        pathKind is PathKind.QualifiedPath.ModuleItemOrEnumVariant
        || pathKind is PathKind.QualifiedPath.FQModuleItem
    ) {
        val qualifierItem = pathKind.qualifier.reference?.resolveFollowingAliases()
        if (qualifierItem is MvEnum) {
            correctedExpectedType = TyAdt.valueOf(qualifierItem)
        }
    }
    val pathEntries = getPathResolveVariants(ctx, pathKind)
    return pathEntries
        .filterEntriesByExpectedType(correctedExpectedType)
}

fun List<ScopeEntry>.filterEntriesByExpectedType(expectedType: Ty?): List<ScopeEntry> {
    return this.filter {
        val entryElement = it.element()
        if (entryElement !is MvEnumVariant) return@filter true

        val expectedEnumItem = (expectedType?.unwrapTyRefs() as? TyAdt)?.adtItem as? MvEnum
        // if expected type is unknown, or not a enum, then we cannot infer enum variants
            ?: return@filter false

        entryElement in expectedEnumItem.variants
    }

}

fun resolveAliases(element: MvNamedElement): MvNamedElement {
    if (element is MvUseAlias) {
        val aliasedPath = element.parentUseSpeck.path
        val resolvedItem = aliasedPath.reference?.resolve()
        if (resolvedItem != null) {
            return resolvedItem
        }
    }
    return element
}

fun getPathResolveVariants(ctx: ResolutionContext, pathKind: PathKind): List<ScopeEntry> {
    return buildList {
        when (pathKind) {
            is PathKind.NamedAddress, is ValueAddress -> Unit
            is PathKind.NamedAddressOrUnqualifiedPath, is PathKind.UnqualifiedPath -> {
                if (Ns.MODULE in pathKind.ns) {
                    // Self::call() as an expression
                    add(ScopeEntry("Self", lazy { ctx.containingModule }, Ns.MODULE))
                }
                // local
                addAll(
                    getEntriesFromWalkingScopes(ctx.path, pathKind.ns)
                )
            }
            is PathKind.QualifiedPath.Module -> {
                val moduleEntries = getModulesAsEntries(ctx, pathKind.address)
                addAll(moduleEntries)
            }
            is PathKind.QualifiedPath -> {
                val qualifiedPathEntries = getQualifiedPathEntries(ctx, pathKind.qualifier)
                    .filterByNs(pathKind.ns)
                addAll(qualifiedPathEntries)
            }
        }
    }
}

/**
 * foo::bar
 * |    |
 * |    [ctx.path]
 * [qualifier]
 */
fun getQualifiedPathEntries(
    ctx: ResolutionContext,
    qualifier: MvPath,
): List<ScopeEntry> {
    val qualifierItem = qualifier.reference?.resolveFollowingAliases()
    return buildList {
        when (qualifierItem) {
            is MvModule -> {
                add(ScopeEntry("Self", lazy { qualifierItem }, Ns.MODULE))
                addAll(qualifierItem.allScopesImportableEntries)
            }
            is MvEnum -> {
                addAll(qualifierItem.variants.asEntries())
            }
            null -> {
                // can be a module, try for named address as a qualifier
                val addressName = qualifier.referenceName ?: return@buildList
                // no Aptos project, cannot resolve by address
                val moveProject = ctx.moveProject ?: return@buildList
                // no such named address
                val address = moveProject.getNamedAddress(addressName) ?: return@buildList
                val moduleEntries = getModulesAsEntries(ctx, address)
                addAll(moduleEntries)
            }
        }
    }
}

class ResolutionContext(val path: MvPath, val isCompletion: Boolean) {
    private var lazyContainingMoveProject: Lazy<MoveProject?> = lazy(NONE) {
        path.moveProject
    }
    val moveProject: MoveProject? get() = lazyContainingMoveProject.value

    private var lazyContainingModule: Lazy<MvModule?> = lazy(NONE) {
        path.containingModule
    }
    val containingModule: MvModule? get() = lazyContainingModule.value

    private var lazyUseSpeck: Lazy<MvUseSpeck?> = lazy(NONE) { path.useSpeck }
    val useSpeck: MvUseSpeck? get() = lazyUseSpeck.value
    val isUseSpeck: Boolean get() = useSpeck != null

    val isCallExpr: Boolean get() = path.rootPath().parent is MvCallExpr
}

fun resolvePath(path: MvPath, expectedType: Ty? = null): List<ScopeEntry> {
    val ctx = ResolutionContext(path, false)

    val referenceName = ctx.path.referenceName ?: return emptyList()
    val kind = ctx.path.pathKind(ctx.isCompletion)
    val entries = getPathResolveVariantsWithExpectedType(ctx, kind, expectedType)
        .filterByName(referenceName)
    // There's a bug in the current Aptos compiler, where it resolves to the module function if available,
    // even if there's a variable with the same name. This code is meant to copy this behaviour.
    // todo: drop it when the bug is fixed
    if (ctx.isCallExpr) {
        val functionEntries = entries.filterByNs(FUNCTIONS)
        return if (functionEntries.isNotEmpty()) {
            functionEntries
        } else {
            entries
        }
    }
    if (entries.size > 1) {
        // we're not at the callable, so drop function entries and see whether we'd get to a single entry
        val nonFunctions = entries.filter { it.ns != Ns.FUNCTION }
        if (nonFunctions.size == 1) {
            return nonFunctions
        }
    }
    return entries
}

