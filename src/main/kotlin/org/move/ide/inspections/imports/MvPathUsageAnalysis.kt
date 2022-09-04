package org.move.ide.inspections.imports

import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.allModuleSpecBlocks
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.ext.moduleSpec

typealias ItemUsages = MutableMap<String, MutableSet<MvNamedElement>>

data class PathUsages(
    val nameUsages: ItemUsages,
    val typeUsages: ItemUsages,
) {
    fun updateFrom(other: PathUsages) {
        nameUsages.putAll(other.nameUsages)
        typeUsages.putAll(other.typeUsages)
    }

    fun all(): ItemUsages {
        val usages = nameUsages.toMutableMap()
        usages.putAll(typeUsages)
        return usages
    }
}

val MvImportsOwner.pathUsages: PathUsages
    get() {
        val localPathUsages = this.localPathUsages()
        when (this) {
            is MvModuleBlock -> {
                for (specBlock in this.module.allModuleSpecBlocks()) {
                    localPathUsages.updateFrom(specBlock.localPathUsages())
                }
            }
            is MvModuleSpecBlock -> {
                val module = this.moduleSpec.module ?: return localPathUsages
                val moduleBlock = module.moduleBlock
                if (moduleBlock != null) {
                    localPathUsages.updateFrom(moduleBlock.localPathUsages())
                }
                for (specBlock in module.allModuleSpecBlocks().filter { it != this }) {
                    localPathUsages.updateFrom(specBlock.localPathUsages())
                }
            }
        }
        return localPathUsages
    }

private fun MvImportsOwner.localPathUsages(): PathUsages {
    return getProjectPsiDependentCache(this) {
        val nameUsages = mutableMapOf<String, MutableSet<MvNamedElement>>()
        val typeUsages = mutableMapOf<String, MutableSet<MvNamedElement>>()
        for (child in it.children) {
            PsiTreeUtil.processElements(child) { element ->
                when {
                    element is MvPathType -> {
                        putUsage(element.path, typeUsages)
                        true
                    }
                    element is MvPath && element.parent !is MvPathType -> {
                        putUsage(element, nameUsages)
                        true
                    }
                    else -> true
                }
            }
        }
        PathUsages(nameUsages, typeUsages)
    }
}

private fun putUsage(element: MvPath, itemUsages: ItemUsages) {
    val moduleRef = element.moduleRef
    when {
        // MODULE::ITEM
        moduleRef != null && moduleRef !is MvFQModuleRef -> {
            val modName = moduleRef.referenceName ?: return
            val targets = moduleRef.reference?.multiResolve().orEmpty()
            if (targets.isEmpty()) {
                itemUsages.putIfAbsent(modName, mutableSetOf())
            } else {
                val items = itemUsages.getOrPut(modName) { mutableSetOf() }
                targets.forEach {
                    items.add(it)
                }
            }
        }
        // ITEM_NAME
        moduleRef == null -> {
            val name = element.referenceName ?: return
            val targets = element.reference?.multiResolve().orEmpty()
            if (targets.isEmpty()) {
                itemUsages.putIfAbsent(name, mutableSetOf())
            } else {
                val items = itemUsages.getOrPut(name) { mutableSetOf() }
                targets.forEach {
                    items.add(it)
                }
            }
        }
    }
}
