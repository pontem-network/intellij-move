package org.move.lang.core.resolve2.ref

import com.intellij.psi.ResolveResult
import org.move.cli.MoveProject
import org.move.lang.core.completion.getOriginalOrSelf
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.resolve.ref.Namespace.*
import org.move.lang.core.resolve2.*
import org.move.lang.core.resolve2.PathKind.ValueAddress
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.moveProject
import kotlin.LazyThreadSafetyMode.NONE

interface InferenceCachedPathElement: MvElement {
    val path: MvPath
}

class MvPath2ReferenceImpl(element: MvPath): MvPolyVariantReferenceBase<MvPath>(element),
                                             MvPath2Reference {

    override fun resolve(): MvNamedElement? =
        rawMultiResolveIfVisible().singleOrNull()?.element as? MvNamedElement

    override fun multiResolve(): List<MvNamedElement> =
        rawMultiResolveIfVisible().mapNotNull { it.element as? MvNamedElement }

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        rawMultiResolve().toTypedArray()

//    fun multiResolveIfVisible(): List<MvElement> =
//        rawMultiResolve().mapNotNull {
//            if (!it.isVisible) return@mapNotNull null
//            it.element
//        }

    fun rawMultiResolveIfVisible(): List<RsPathResolveResult<MvElement>> =
        rawMultiResolve().filter { it.isVisible }

    fun rawMultiResolve(): List<RsPathResolveResult<MvElement>> =
        rawMultiResolveUsingInferenceCache() ?: rawCachedMultiResolve()

    private fun rawMultiResolveUsingInferenceCache(): List<RsPathResolveResult<MvElement>>? {
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

    private fun getResolvedPathFromInference(path: MvPath, msl: Boolean): List<RsPathResolveResult<MvElement>>? {
        // Path resolution is cached, but sometimes path changes so much that it can't be retrieved
        // from cache anymore. In this case we need to get the old path.
        val originalPath = path.getOriginalOrSelf()
        return originalPath.inference(msl)?.getResolvedPath(originalPath)
            ?.map {
                RsPathResolveResult(it.element, it.isVisible)
            }
    }

    private fun rawCachedMultiResolve(): List<RsPathResolveResult<MvElement>> {
        return MvResolveCache
            .getInstance(element.project)
            .resolveWithCaching(element, ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE, Resolver)
            .orEmpty()
    }

    private object Resolver: (MvElement) -> List<RsPathResolveResult<MvElement>> {
        override fun invoke(path: MvElement): List<RsPathResolveResult<MvElement>> {
            // should not really happen
            if (path !is MvPath) return emptyList()
            val resolutionCtx = ResolutionContext(path, isCompletion = false)
            return resolvePath(resolutionCtx, path)
        }
    }
}

fun processPathResolveVariantsWithExpectedType(
    ctx: ResolutionContext,
    pathKind: PathKind,
    expectedType: Ty?,
    processor: RsResolveProcessor
): Boolean {
    val expectedTypeFilterer = filterEnumVariantsByExpectedType(expectedType, processor)
    return processPathResolveVariants(
        ctx,
        pathKind,
        processor = expectedTypeFilterer
    )
}

fun filterEnumVariantsByExpectedType(expectedType: Ty?, processor: RsResolveProcessor): RsResolveProcessor {
    if (expectedType == null) return processor

    val enumItem = (expectedType as? TyAdt)?.item as? MvEnum
    if (enumItem == null) return processor

    val allowedVariants = enumItem.variants
    return processor.wrapWithFilter {
        val element = it.element
        element !is MvEnumVariant || element in allowedVariants
    }
}

//fun resolveAliases(processor: RsResolveProcessor): RsResolveProcessor =
//    processor.wrapWithMapper { e: ScopeEntry ->
//        val visEntry = e as? ScopeEntryWithVisibility ?: return@wrapWithMapper e
//        val element = visEntry.element
//        val unaliased = resolveAliases(element)
//        visEntry.copy(element = unaliased)
////        if (element is MvUseAlias) {
////            val aliasedPath = element.parentUseSpeck.path
////            val resolvedItem = aliasedPath.reference?.resolve()
////            if (resolvedItem != null) {
////                return@wrapWithMapper visEntry.copy(element = resolvedItem)
////            }
////        }
////        e
//    }

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

fun processPathResolveVariants(
    ctx: ResolutionContext,
    pathKind: PathKind,
    processor: RsResolveProcessor
): Boolean {
    return when (pathKind) {
        is PathKind.NamedAddress, is ValueAddress -> false
        is PathKind.NamedAddressOrUnqualifiedPath, is PathKind.UnqualifiedPath -> {
            if (MODULE in pathKind.ns) {
                // Self::
                if (processor.lazy("Self", MODULES) { ctx.containingModule }) return true
            }
            // local
            processNestedScopesUpwards(ctx.element, pathKind.ns, ctx, processor)
        }
        is PathKind.QualifiedPath.Module -> {
            processModulePathResolveVariants(ctx, pathKind.address, processor)
        }
        is PathKind.QualifiedPath -> {
            processQualifiedPathResolveVariants(ctx, pathKind.ns, pathKind.qualifier, processor)
        }
    }
}

/**
 * foo::bar
 * |    |
 * |    [path]
 * [qualifier]
 */
fun processQualifiedPathResolveVariants(
    ctx: ResolutionContext,
    ns: Set<Namespace>,
    qualifier: MvPath,
    processor: RsResolveProcessor
): Boolean {
    val resolvedQualifier = qualifier.reference?.resolveFollowingAliases()
    if (resolvedQualifier == null) {
        if (MODULE in ns) {
            // can be module, try for named address as a qualifier
            val addressName = qualifier.referenceName ?: return false
            val address = ctx.moveProject?.getNamedAddressTestAware(addressName) ?: return false
            if (processModulePathResolveVariants(ctx, address, processor)) return true
        }
        return false
    }
    if (resolvedQualifier is MvModule) {
        if (processor.process("Self", MODULES, resolvedQualifier)) return true

        val module = resolvedQualifier
        if (processItemDeclarations(module, ns, processor)) return true
    }
    if (resolvedQualifier is MvEnum) {
        if (processEnumVariantDeclarations(resolvedQualifier, ns, processor)) return true
    }
    return false
}

class ResolutionContext(val element: MvElement, val isCompletion: Boolean) {
    private var lazyContainingMoveProject: Lazy<MoveProject?> = lazy(NONE) {
        element.moveProject
    }
    val moveProject: MoveProject? get() = lazyContainingMoveProject.value

    private var lazyContainingModule: Lazy<MvModule?> = lazy(NONE) {
        element.containingModule
    }
    val containingModule: MvModule? get() = lazyContainingModule.value

    private var lazyUseSpeck: Lazy<MvUseSpeck?> = lazy(NONE) { path?.useSpeck }
    val useSpeck: MvUseSpeck? get() = lazyUseSpeck.value
    val isUseSpeck: Boolean get() = useSpeck != null

    val methodOrPath: MvMethodOrPath? get() = element as? MvMethodOrPath
    val path: MvPath? get() = element as? MvPath

    val isSpecOnlyExpr: Boolean get() = element.hasAncestor<MvSpecOnlyExpr>()
}

fun resolvePathRaw(path: MvPath, expectedType: Ty? = null): List<ScopeEntry> {
    val ctx = ResolutionContext(path, false)
    val kind = path.pathKind()
    val resolveVariants =
        collectResolveVariantsAsScopeEntries(path.referenceName) {
            processPathResolveVariantsWithExpectedType(ctx, kind, expectedType, it)
        }
    return resolveVariants
}

private fun resolvePath(
    ctx: ResolutionContext,
    path: MvPath,
): List<RsPathResolveResult<MvElement>> {
    val pathKind = path.pathKind()
    val result =
        // matches resolve variants against referenceName from path
        collectMethodOrPathResolveVariants(path, ctx) {
            // actually emits resolve variants
            processPathResolveVariantsWithExpectedType(ctx, pathKind, expectedType = null, it)
//            processPathResolveVariants(ctx, pathKind, it)
        }
    return result
//    return when (result.size) {
//        0 -> emptyList()
//        1 -> listOf(result.single())
//        else -> result
//    }
}

