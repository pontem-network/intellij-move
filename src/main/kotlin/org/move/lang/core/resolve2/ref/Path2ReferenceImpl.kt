package org.move.lang.core.resolve2.ref

import com.intellij.psi.ResolveResult
import org.move.cli.MoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.allowedNamespaces
import org.move.lang.core.psi.ext.qualifier
import org.move.lang.core.psi.ext.rootPath
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.resolve2.processAddressPathResolveVariants
import org.move.lang.core.resolve2.processItemDeclarations
import org.move.lang.core.resolve2.processNestedScopesUpwards
import org.move.lang.core.resolve2.ref.RsPathResolveKind.*
import org.move.lang.core.types.Address
import org.move.lang.moveProject
import kotlin.LazyThreadSafetyMode.NONE

class Path2ReferenceImpl(element: MvPath):
    MvPolyVariantReferenceBase<MvPath>(element), MvPath2Reference {

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

//    fun rawMultiResolve(): List<RsPathResolveResult<MvElement>> = Resolver.invoke(this.element)
    fun rawMultiResolve(): List<RsPathResolveResult<MvElement>> = rawCachedMultiResolve()

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
                path = path,
                contextScopeInfo = ContextScopeInfo.from(path)
            )
            return resolvePath(ctx, path, classifyPath(path))
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
            processNestedScopesUpwards(ctx.path, pathKind.ns, ctx, contextScopeAwareProcessor)
        }
        is ModulePath -> {
            // 0x1::bar
            processAddressPathResolveVariants(
                ctx.path,
                ctx.moveProject,
                pathKind.address,
                contextScopeAwareProcessor
            )
        }
        is QualifiedPath -> {
            // foo::bar
            processQualifiedPathResolveVariants(ctx, pathKind.ns, pathKind.path, pathKind.qualifier, processor)
        }
        is NamedAddressPath -> {
            return false
        }
    }
}

fun processPathResolveVariants(
    path: MvPath,
    contextScopeInfo: ContextScopeInfo = ContextScopeInfo.from(path),
    processor: RsResolveProcessor,
): Boolean {
    val ctx = PathResolutionContext(path, contextScopeInfo = contextScopeInfo)
    val pathKind = classifyPath(path)
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

class PathResolutionContext(
    val path: MvPath,
    val contextScopeInfo: ContextScopeInfo,
) {
    private var lazyContainingMoveProject: Lazy<MoveProject?> = lazy(NONE) {
        path.moveProject
    }
    val moveProject: MoveProject? get() = lazyContainingMoveProject.value

    private var lazyContainingModule: Lazy<MvModule?> = lazy(NONE) {
        path.containingModule
    }
    val containingModule: MvModule? get() = lazyContainingModule.value

//    var lazyContainingModInfo: Lazy<ModInfo?> = lazy(NONE) {
//        val module = containingModule
//        getModInfo(module)
//    }

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

