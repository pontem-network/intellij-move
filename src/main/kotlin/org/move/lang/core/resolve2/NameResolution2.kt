package org.move.lang.core.resolve2

import com.intellij.psi.search.GlobalSearchScope
import org.move.cli.MoveProject
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.containingMoveFile
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve2.ref.PathResolutionContext
import org.move.lang.core.types.address
import org.move.lang.index.MvNamedElementIndex

fun processNestedScopesUpwards(
    scopeStart: MvElement,
    ns: Set<Namespace>,
    ctx: PathResolutionContext,
    processor: RsResolveProcessor
): Boolean {
    return walkUpThroughScopes(
        scopeStart,
        stopAfter = { it is MvModule }
    ) { cameFrom, scope ->
        processItemsInScope(
            scope, cameFrom, ns, ctx.contextScopeInfo, processor
        )
//        if (scope !is MvCodeBlock && scope is MvItemsOwner) {
//            if (processItemDeclarations(scope, ns, addImports = true, processor)) return@walkUpThroughScopes true
//            false
//        } else {
//
//        }
    }
}

fun processAddressPathResolveVariants(
    element: MvElement,
    moveProject: MoveProject?,
    address: String,
    processor: RsResolveProcessor,
): Boolean {
    // if no project, cannot use the index
    if (moveProject == null) return false

    val equalAddressProcessor = processor.wrapWithFilter { e ->
        val candidate = e.element as? MvModule ?: return@wrapWithFilter false
        val candidateAddress = candidate.address(moveProject)?.canonicalValue(moveProject)
        address == candidateAddress
    }

    // search modules in the current file first
    val currentFile = element.containingMoveFile ?: return false
    for (module in currentFile.modules()) {
        if (equalAddressProcessor.process(module)) return true
    }

    val project = element.project
    val currentFileScope = GlobalSearchScope.fileScope(currentFile)
    val searchScope =
        moveProject.searchScope().intersectWith(GlobalSearchScope.notScope(currentFileScope))

    var stop = false
    for (name in processor.names.orEmpty()) {
        MvNamedElementIndex
            .processElementsByName(project, name, searchScope) {
                stop = equalAddressProcessor.process(it)
                // true to continue processing, if .process does not find anything, it returns false
                !stop
            }
        if (stop) return true
    }

    return false
}
