package org.move.lang.core.resolve2.ref

import com.intellij.psi.ResolveResult
import org.move.cli.MoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvMethodOrPath
import org.move.lang.core.psi.ext.useSpeck
import org.move.lang.core.psi.ext.variants
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.resolve2.*
import org.move.lang.core.resolve2.PathKind.NamedAddress
import org.move.lang.core.resolve2.PathKind.ValueAddress
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
            val resolutionCtx = ResolutionContext(path, isCompletion = false)
            return resolvePath(resolutionCtx, path)
        }
    }
}

fun processPathResolveVariants(
    ctx: ResolutionContext,
    pathKind: PathKind,
    processor: RsResolveProcessor
): Boolean {
    return when (pathKind) {
        is NamedAddress, is ValueAddress -> false
        is PathKind.UnqualifiedPath -> {
            if (Namespace.MODULE in pathKind.ns) {
                // Self::
                if (processor.lazy("Self") { ctx.containingModule }) return true
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
        if (Namespace.MODULE in ns) {
            // can be module, try for named address as a qualifier
            val addressName = qualifier.referenceName ?: return false
            val address = ctx.moveProject?.getNamedAddressTestAware(addressName) ?: return false
            if (processModulePathResolveVariants(ctx, address, processor)) return true
        }
        return false
    }
    if (resolvedQualifier is MvModule) {
        if (processor.process("Self", resolvedQualifier)) return true

        val module = resolvedQualifier
        if (processItemDeclarations(module, ns, processor)) return true
    }
    if (resolvedQualifier is MvEnum) {
        if (processor.processAll(resolvedQualifier.variants)) return true
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
}

//// todo: use in inference later
//fun resolvePathRaw(path: MvPath): List<ScopeEntry> {
//    return collectResolveVariantsAsScopeEntries(path.referenceName) {
//        processPathResolveVariants(path, it)
//    }
//}

private fun resolvePath(
    ctx: ResolutionContext,
    path: MvPath,
//    kind: RsPathResolveKind
): List<RsPathResolveResult<MvElement>> {
    val pathKind = path.pathKind()
    val result =
        // matches resolve variants against referenceName from path
        collectMethodOrPathResolveVariants(path, ctx) {
            // actually emits resolve variants
            processPathResolveVariants(ctx, pathKind, it)
        }
    return result
//    return when (result.size) {
//        0 -> emptyList()
//        1 -> listOf(result.single())
//        else -> result
//    }
}

