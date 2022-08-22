package org.move.ide.inspections.imports

import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.allModuleSpecBlocks
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.ext.moduleSpec

typealias PathUsages = MutableMap<String, MutableSet<MvNamedElement>>

val MvImportsOwner.pathUsages: PathUsages
    get() {
        val localPathUsages = this.localPathUsages()
        when (this) {
            is MvModuleBlock -> {
                for (specBlock in this.module.allModuleSpecBlocks()) {
                    localPathUsages.putAll(specBlock.localPathUsages())
                }
            }
            is MvModuleSpecBlock -> {
                val module = this.moduleSpec.module ?: return localPathUsages
                val moduleBlock = module.moduleBlock
                if (moduleBlock != null) {
                    localPathUsages.putAll(moduleBlock.localPathUsages())
                }
                for (specBlock in module.allModuleSpecBlocks().filter { it != this }) {
                    localPathUsages.putAll(specBlock.localPathUsages())
                }
            }
        }
        return localPathUsages
    }

private fun MvImportsOwner.localPathUsages(): PathUsages {
    return getProjectPsiDependentCache(this) {
        val map = mutableMapOf<String, MutableSet<MvNamedElement>>()
        for (child in it.children) {
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
        map
    }
}
