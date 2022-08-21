package org.move.ide.inspections.imports

import com.intellij.openapi.util.Key
import com.intellij.psi.util.*
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.fqModule
import org.move.lang.index.MvModuleSpecIndex
import org.move.lang.moduleSpecs
import org.move.lang.moveProject

typealias PathUsages = Map<String, MutableSet<MvNamedElement>>

private val PATH_USAGE_KEY: Key<CachedValue<PathUsages>> = Key.create("PATH_USAGE_KEY")

val MvImportsOwner.pathUsages: PathUsages
    get() = CachedValuesManager.getCachedValue(this, PATH_USAGE_KEY) {
        val usages = calculatePathUsages(this)
        CachedValueProvider.Result.create(usages, PsiModificationTracker.MODIFICATION_COUNT)
    }

private fun calculatePathUsages(owner: MvImportsOwner): PathUsages {
    val map = hashMapOf<String, MutableSet<MvNamedElement>>()

    for (child in owner.children) {
        PsiTreeUtil.processElements(child) { el ->
            if (el !is MvPath) return@processElements true

            val moduleRef = el.moduleRef
            when {
                // MODULE::ITEM
                moduleRef != null && moduleRef !is MvFQModuleRef -> {
                    val modName = moduleRef.referenceName ?: return@processElements true
                    val targets = moduleRef.reference?.multiResolve().orEmpty()
                    if (targets.isEmpty()) {
                        map.putIfAbsent(modName, mutableSetOf())
                    } else {
                        val items = map.getOrPut(modName) { mutableSetOf() }
                        targets.forEach {
                            items.add(it)
                        }
                    }
                    true
                }
                // ITEM_NAME
                moduleRef == null -> {
                    val name = el.referenceName ?: return@processElements true
                    val targets = el.reference?.multiResolve().orEmpty()
                    if (targets.isEmpty()) {
                        map.putIfAbsent(name, mutableSetOf())
                    } else {
                        val items = map.getOrPut(name) { mutableSetOf() }
                        targets.forEach {
                            items.add(it)
                        }
                    }
                    true
                }
                else -> true
            }
        }
    }

    if (owner is MvModuleBlock) {
        val currentModule = (owner.parent as MvModule).fqModule() ?: return map
        val moveProject = owner.moveProject ?: return map
        val specFiles =
            MvModuleSpecIndex.moduleSpecFiles(owner.project, currentModule.name, moveProject.searchScope())
        for (specFile in specFiles) {
            val moduleSpecs = specFile.moduleSpecs().filter {
                val module = it.fqModuleRef?.reference?.resolve() as? MvModule ?: return@filter false
                currentModule == module.fqModule()
            }
            for (specBlock in moduleSpecs.mapNotNull { it.moduleSpecBlock }) {
                map.putAll(specBlock.pathUsages)
            }
        }
//        // find all spec modules for this module
//        owner.moveProject?.processMoveFiles { file ->
//            val moduleSpecs = file.moduleSpecs().filter {
//                val module = it.fqModuleRef?.reference?.resolve() as? MvModule ?: return@filter false
//                currentModule == module.fqModule()
//            }
//            for (specBlock in moduleSpecs.mapNotNull { it.moduleSpecBlock }) {
//                map.putAll(specBlock.pathUsages)
//            }
//            true
//        }
    }

    return map
}

//private fun collectPathUsagesFromModuleSpecs(
//    file: MoveFile,
//    pathUsages: MutableMap<String, MutableSet<MvNamedElement>>
//) {
//    val moduleSpecs = file.moduleSpecs().filter {
//        val module = it.fqModuleRef?.reference?.resolve() as? MvModule ?: return@filter false
//        currentModule == module.fqModule()
//    }
//    for (block in moduleSpecs.mapNotNull { it.moduleSpecBlock }) {
//        pathUsages.putAll(block.pathUsages.map)
//    }
//}
