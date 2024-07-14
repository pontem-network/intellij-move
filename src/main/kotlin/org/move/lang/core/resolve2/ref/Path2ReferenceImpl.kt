package org.move.lang.core.resolve2.ref

import com.intellij.psi.ResolveResult
import org.move.cli.MoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.allowedNamespaces
import org.move.lang.core.psi.ext.qualifier
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.resolve2.*
import org.move.lang.core.resolve2.ref.RsPathResolveKind.*
import org.move.lang.core.types.Address
import org.move.lang.moveProject
import kotlin.LazyThreadSafetyMode.NONE

class Path2ReferenceImpl(element: MvPath):
    MvPolyVariantReferenceBase<MvPath>(element), MvPath2Reference {

//    private fun rawMultiResolveUsingInferenceCache(): List<RsPathResolveResult<MvElement>>? {
//        val msl = element.isMsl()
//        return element.inference(msl)?.getResolvedPath(element)?.map { result ->
//            RsPathResolveResult(result.element, result.isVisible)
//        }
//    }

//    override fun multiResolveInner(): List<MvNamedElement> {
//        val referenceName = element.referenceName ?: return emptyList()
//        val resolveVariants = collectResolveVariantsAsScopeEntries(referenceName) {
//            processPathResolveVariants(element, it)
//        }
//        return resolveVariants.mapNotNull { it.element as? MvNamedElement }
//    }

    override fun resolve(): MvNamedElement? = rawMultiResolve().singleOrNull()?.element as? MvNamedElement

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        rawMultiResolve().toTypedArray()

    override fun multiResolve(): List<MvNamedElement> =
        rawMultiResolve().mapNotNull { it.element as? MvNamedElement }

    override fun multiResolveIfVisible(): List<MvElement> =
        rawMultiResolve().mapNotNull {
            if (!it.isVisible) return@mapNotNull null
            it.element
        }

    override fun rawMultiResolve(): List<RsPathResolveResult<MvElement>> = Resolver.invoke(this.element)
//    override fun rawMultiResolve(): List<RsPathResolveResult<MvElement>> = rawCachedMultiResolve()

    private fun rawCachedMultiResolve(): List<RsPathResolveResult<MvElement>> {
        val rawResult = MvResolveCache.getInstance(element.project)
            .resolveWithCaching(element, ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE, Resolver)
        return rawResult.orEmpty()
    }

    private object Resolver: (MvElement) -> List<RsPathResolveResult<MvElement>> {
        override fun invoke(path: MvElement): List<RsPathResolveResult<MvElement>> {
            // should not really happen
            if (path !is MvPath) return emptyList()

            val ctx = PathResolutionContext(
                context = path,
                contextScopeInfo = ContextScopeInfo.from(path)
            )
            val result = resolvePath(ctx, path, ctx.classifyPath(path))
            return result
        }
    }
}

fun processPathResolveVariants(
    ctx: PathResolutionContext,
    pathKind: RsPathResolveKind,
    processor: RsResolveProcessor
): Boolean {
    val contextScopeAwareProcessor = processor.wrapWithFilter {
        val element = it.element
        element is MvNamedElement && ctx.contextScopeInfo.matches(element)
    }
    return when (pathKind) {
        is UnqualifiedPath -> {
            // Self::
            if (processor.lazy("Self") { ctx.containingModule }) return true
            // local
            processNestedScopesUpwards(ctx.context, pathKind.ns, ctx, contextScopeAwareProcessor)
        }
        is AddressPath -> {
            // 0x1::bar
            processAddressPathResolveVariants(
                ctx.context,
                ctx.moveProject,
                pathKind.canonicalAddress,
                contextScopeAwareProcessor
            )
        }
        is QualifiedPath -> {
            // foo::bar
            processQualifiedPathResolveVariants(ctx, pathKind.ns, pathKind.path, pathKind.qualifier, processor)
        }
    }
}

fun processPathResolveVariants(
    path: MvPath,
    processor: RsResolveProcessor,
): Boolean {
    val ctx = PathResolutionContext(path, contextScopeInfo = ContextScopeInfo.from(path))
    val pathKind = ctx.classifyPath(path)
    return processPathResolveVariants(ctx, pathKind, processor)
}

/**
 * foo::bar
 * |    |
 * |    [path]
 * [qualifier]
 */
fun processQualifiedPathResolveVariants(
    ctx: PathResolutionContext,
    ns: Set<Namespace>,
    path: MvPath,
    qualifier: MvPath,
    processor: RsResolveProcessor
): Boolean {
    val resolvedQualifier = qualifier.reference?.resolveFollowingAliases()
    if (resolvedQualifier != null) {
        val result =
            processQualifiedPathResolveVariants1(ctx, ns, qualifier, resolvedQualifier, path, processor)
        if (result) return true
    }
    return false
}

private fun processQualifiedPathResolveVariants1(
    ctx: PathResolutionContext,
    ns: Set<Namespace>,
    qualifier: MvPath,
    resolvedQualifier: MvElement,
    path: MvPath,
    processor: RsResolveProcessor
): Boolean {
    if (resolvedQualifier is MvModule) {
        if (processor.process("Self", resolvedQualifier)) return true

        val block = resolvedQualifier.moduleBlock
        if (block != null) {
            if (processItemDeclarations(block, ns, processor)) return true
        }
    }
    return false
}

sealed class RsPathResolveKind {
    /** A path consist of a single identifier, e.g. `foo` */
    data class UnqualifiedPath(val ns: Set<Namespace>): RsPathResolveKind()

    /** `bar` in `foo::bar` or `use foo::{bar}` */
    class QualifiedPath(
        val path: MvPath,
        val qualifier: MvPath,
        val ns: Set<Namespace>,
    ): RsPathResolveKind()

    /** bar in `0x1::bar` */
    class AddressPath(
        val path: MvPath,
        val canonicalAddress: String,
    ): RsPathResolveKind()
}

class PathResolutionContext(
    val context: MvElement,
    val contextScopeInfo: ContextScopeInfo,
) {
    private var lazyContainingMoveProject: Lazy<MoveProject?> = lazy(NONE) {
        context.moveProject
    }
    val moveProject: MoveProject? get() = lazyContainingMoveProject.value

    private var lazyContainingModule: Lazy<MvModule?> = lazy(NONE) {
        context.containingModule
    }
    val containingModule: MvModule? get() = lazyContainingModule.value
    var lazyContainingModInfo: Lazy<ModInfo?> = lazy(NONE) {
        containingModule?.let { getModInfo(it) }
    }

    fun classifyPath(path: MvPath): RsPathResolveKind {
        val qualifier = path.qualifier
        val ns = path.allowedNamespaces()
        if (qualifier == null) {
            return UnqualifiedPath(ns)
        }

        val pathAddress = qualifier.pathAddress
        return when {
            pathAddress != null -> {
                val address = Address.Value(pathAddress.text)
                val canonical = address.addressLit().canonical()
                AddressPath(path, canonical)
            }
            else -> QualifiedPath(path, qualifier, ns)
        }
    }
}

//// todo: use in inference later
//fun resolvePathRaw(path: MvPath): List<ScopeEntry> {
//    return collectResolveVariantsAsScopeEntries(path.referenceName) {
//        processPathResolveVariants(path, it)
//    }
//}

private fun resolvePath(
    ctx: PathResolutionContext,
    path: MvPath,
    kind: RsPathResolveKind
): List<RsPathResolveResult<MvElement>> {
    val result =
        // matches resolve variants against referenceName from path
        collectPathResolveVariants(ctx, path) {
            // actually emits resolve variants
            processPathResolveVariants(ctx, kind, it)
        }
    return result
//    return when (result.size) {
//        0 -> emptyList()
//        1 -> listOf(result.single())
//        else -> result
//    }
}

