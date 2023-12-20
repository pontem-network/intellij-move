package org.move.lang.core.resolve

import com.intellij.psi.search.GlobalSearchScope
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.address
import org.move.lang.index.MvNamedElementIndex
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull
import org.move.stdext.wrapWithList

data class ItemVis(
    val namespaces: Set<Namespace>,
    val visibilities: Set<Visibility>,
    val mslLetScope: MslLetScope,
    val itemScopes: Set<ItemScope>,
) {
    val isMsl get() = mslLetScope != MslLetScope.NONE
}

fun processItems(
    element: MvElement,
    namespaces: Set<Namespace>,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    return walkUpThroughScopes(
        element,
        stopAfter = { it is MvModule }
    ) { cameFrom, scope ->
        processItemsInScope(
            scope, cameFrom, namespaces, itemVis, processor
        )
    }
}

fun resolveLocalItem(
    element: MvReferenceElement,
    namespaces: Set<Namespace>
): List<MvNamedElement> {
    val itemVis = ItemVis(
        namespaces,
        mslLetScope = element.mslLetScope,
        visibilities = Visibility.local(),
        itemScopes = element.itemScopes,
    )
    val referenceName = element.referenceName
    var resolved: MvNamedElement? = null
    processItems(element, namespaces, itemVis) {
        if (it.name == referenceName) {
            resolved = it.element
            return@processItems true
        }
        return@processItems false
    }
    return resolved.wrapWithList()
}

// go from local MODULE reference to corresponding FqModuleRef (from import)
fun resolveIntoFQModuleRefInUseSpeck(moduleRef: MvModuleRef): MvFQModuleRef? {
    check(moduleRef !is MvFQModuleRef) { "Should be handled on the upper level" }

    // module refers to ModuleImport
    var resolved = resolveLocalItem(moduleRef, setOf(Namespace.MODULE)).firstOrNull()
    if (resolved is MvUseAlias) {
        resolved = resolved.moduleUseSpeck ?: resolved.useItem
    }
    if (resolved is MvUseItem && resolved.isSelf) {
        return resolved.useSpeck().fqModuleRef
    }
//    if (resolved !is MvModuleUseSpeck) return null
    return (resolved as? MvModuleUseSpeck)?.fqModuleRef
//    return resolved.fqModuleRef
}

fun processQualItem(
    item: MvNamedElement,
    namespaces: Set<Namespace>,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    val matched = when {
        item is MvModule && Namespace.MODULE in namespaces
                || item is MvStruct && Namespace.TYPE in namespaces
                || item is MvSchema && Namespace.SCHEMA in namespaces ->
            processor.match(itemVis, item)

        item is MvFunction && Namespace.FUNCTION in namespaces -> {
            if (item.isTest) return false
            for (vis in itemVis.visibilities) {
                when {
                    vis is Visibility.Public
                            && item.visibility == FunctionVisibility.PUBLIC -> processor.match(itemVis, item)

                    vis is Visibility.PublicScript
                            && item.visibility == FunctionVisibility.PUBLIC_SCRIPT ->
                        processor.match(itemVis, item)

                    vis is Visibility.PublicFriend && item.visibility == FunctionVisibility.PUBLIC_FRIEND -> {
                        val itemModule = item.module ?: return false
                        val currentModule = vis.currentModule.element ?: return false
                        if (currentModule.fqModule() in itemModule.declaredFriendModules) {
                            processor.match(itemVis, item)
                        }
                    }

                    vis is Visibility.Internal -> processor.match(itemVis, item)
                }
            }
            false
        }
        else -> false
    }
    return matched
}

fun processFileItems(
    file: MoveFile,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    for (module in file.modules()) {
        if (
            Namespace.MODULE in itemVis.namespaces
            && processor.match(itemVis, module)
        ) {
            return true
        }
        if (processModuleInnerItems(module, itemVis, processor)) return true
    }
    return false
}

fun processFQModuleRef(
    fqModuleRef: MvFQModuleRef,
    processor: MatchingProcessor<MvModule>,
) {
    val itemVis = ItemVis(
        namespaces = setOf(Namespace.MODULE),
        visibilities = Visibility.local(),
        mslLetScope = fqModuleRef.mslLetScope,
        itemScopes = fqModuleRef.itemScopes,
    )
    val moveProj = fqModuleRef.moveProject ?: return
    val refAddressText = fqModuleRef.addressRef.address(moveProj)?.canonicalValue(moveProj)

    val moduleProcessor = MatchingProcessor<MvNamedElement> {
        val entry = ScopeItem(it.name, it.element as MvModule)
        val modAddressText = entry.element.address(moveProj)?.canonicalValue(moveProj)
        if (modAddressText != refAddressText)
            return@MatchingProcessor false
        processor.match(entry)
    }

    // first search modules in the current file
    val currentFile = fqModuleRef.containingMoveFile ?: return
    var stopped = processFileItems(currentFile, itemVis, moduleProcessor)
    if (stopped) return

    moveProj.processMoveFiles { moveFile ->
        // skip current file as it's processed already
        if (moveFile.toNioPathOrNull() == currentFile.toNioPathOrNull())
            return@processMoveFiles true
        stopped = processFileItems(moveFile, itemVis, moduleProcessor)
        // if not resolved, returns true to indicate that next file should be tried
        !stopped
    }
}

fun processFQModuleRef(
    moduleRef: MvFQModuleRef,
    target: String,
    processor: MatchingProcessor<MvModule>,
) {
    val project = moduleRef.project
    val moveProj = moduleRef.moveProject ?: return
    val refAddress = moduleRef.addressRef.address(moveProj)?.canonicalValue(moveProj)

    val matchModule = MatchingProcessor<MvNamedElement> {
        val entry = ScopeItem(it.name, it.element as MvModule)
        // TODO: check belongs to the current project
        val modAddress = entry.element.address(moveProj)?.canonicalValue(moveProj)

        if (modAddress != refAddress) return@MatchingProcessor false
        processor.match(entry)
    }

    val namespaces = setOf(Namespace.MODULE)
    val itemVis = ItemVis(
        namespaces = namespaces,
        visibilities = Visibility.local(),
        mslLetScope = moduleRef.mslLetScope,
        itemScopes = moduleRef.itemScopes,
    )
    // search modules in the current file first
    val currentFile = moduleRef.containingMoveFile ?: return
    val stopped = processFileItems(currentFile, itemVis, matchModule)
    if (stopped) return

    val currentFileScope = GlobalSearchScope.fileScope(currentFile)
    val searchScope =
        moveProj.searchScope().intersectWith(GlobalSearchScope.notScope(currentFileScope))

    MvNamedElementIndex
        .processElementsByName(project, target, searchScope) {
            val matched = processQualItem(it, namespaces, itemVis, matchModule)
            if (matched) return@processElementsByName false

            true
        }
}

fun walkUpThroughScopes(
    start: MvElement,
    stopAfter: (MvElement) -> Boolean,
    handleScope: (cameFrom: MvElement, scope: MvElement) -> Boolean,
): Boolean {
    var cameFrom = start
    var scope = start.context as MvElement?
    while (scope != null) {
        if (handleScope(cameFrom, scope)) return true

        // walk all items in original module block
        if (scope is MvModuleBlock) {
            // handle spec module {}
            if (handleModuleItemSpecsInBlock(cameFrom, scope, handleScope)) return true
            // walk over all spec modules
            for (moduleSpec in scope.module.allModuleSpecs()) {
                val moduleSpecBlock = moduleSpec.moduleSpecBlock ?: continue
                if (handleScope(cameFrom, moduleSpecBlock)) return true
                if (handleModuleItemSpecsInBlock(cameFrom, moduleSpecBlock, handleScope)) return true
            }
        }

        if (scope is MvModuleSpecBlock) {
            val moduleBlock = scope.moduleSpec.moduleItem?.moduleBlock
            if (moduleBlock != null) {
                cameFrom = scope
                scope = moduleBlock
                continue
            }
        }

        if (stopAfter(scope)) break

        cameFrom = scope
        scope = scope.context as? MvElement
    }

    return false
}

private fun handleModuleItemSpecsInBlock(
    cameFrom: MvElement,
    block: MvElement,
    handleScope: (cameFrom: MvElement, scope: MvElement) -> Boolean
): Boolean {
    val moduleItemSpecs = when (block) {
        is MvModuleBlock -> block.moduleItemSpecList
        is MvModuleSpecBlock -> block.moduleItemSpecList
        else -> emptyList()
    }
    for (moduleItemSpec in moduleItemSpecs.filter { it != cameFrom }) {
        val itemSpecBlock = moduleItemSpec.itemSpecBlock ?: continue
        if (handleScope(cameFrom, itemSpecBlock)) return true
    }
    return false
}
