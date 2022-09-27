package org.move.ide.inspections.imports

import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ItemScope

typealias ItemUsages = MutableMap<String, MutableSet<MvNamedElement>>

data class ScopePathUsages(
    val nameUsages: ItemUsages,
    val typeUsages: ItemUsages,
) {
    fun updateFrom(other: ScopePathUsages) {
        nameUsages.putAll(other.nameUsages)
        typeUsages.putAll(other.typeUsages)
    }

    fun all(): ItemUsages {
        val usages = nameUsages.toMutableMap()
        usages.putAll(typeUsages)
        return usages
    }
}

data class PathUsages(
    val mainUsages: ScopePathUsages,
    val testUsages: ScopePathUsages
) {
    fun updateFrom(other: PathUsages) {
        mainUsages.updateFrom(other.mainUsages)
        testUsages.updateFrom(other.testUsages)
    }

    fun get(itemScope: ItemScope): ScopePathUsages {
        return when (itemScope) {
            ItemScope.MAIN -> mainUsages
            ItemScope.TEST -> testUsages
        }
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

data class ImportInfo(
    val mainImports: Map<String, MvElement>,
    val testImports: Map<String, MvElement>
) {
    fun getImports(itemScope: ItemScope): Map<String, MvElement> {
        return when (itemScope) {
            ItemScope.MAIN -> mainImports
            ItemScope.TEST -> testImports
        }
    }
}

val MvImportsOwner.importInfo: ImportInfo
    get() {
        return getProjectPsiDependentCache(this) { importsOwner ->
            val mainImports = mutableMapOf<String, MvElement>()
            val testImports = mutableMapOf<String, MvElement>()
            for (useStmt in importsOwner.useStmtList) {
                val scopeImports = if (useStmt.isTestOnly) testImports else mainImports
                for (useItem in useStmt.childUseItems) {
                    val name = useItem.name ?: continue
                    scopeImports[name] = useItem
                }
                val moduleSpeck = useStmt.moduleUseSpeck ?: continue
                val name = moduleSpeck.name ?: continue
                scopeImports[name] = moduleSpeck
            }
            ImportInfo(mainImports, testImports)
        }
    }

private fun MvImportsOwner.localPathUsages(): PathUsages {
    return getProjectPsiDependentCache(this) { importsOwner ->

        val mainNameUsages = mutableMapOf<String, MutableSet<MvNamedElement>>()
        val mainTypeUsages = mutableMapOf<String, MutableSet<MvNamedElement>>()
        val testNameUsages = mutableMapOf<String, MutableSet<MvNamedElement>>()
        val testTypeUsages = mutableMapOf<String, MutableSet<MvNamedElement>>()

        for (child in importsOwner.children) {
            PsiTreeUtil.processElements(child, MvPath::class.java) { path ->
                val nameUsages =
                    if (path.itemScope == ItemScope.TEST) testNameUsages else mainNameUsages
                val typeUsages =
                    if (path.itemScope == ItemScope.TEST) testTypeUsages else mainTypeUsages
                when {
                    path.moduleRef != null -> putUsage(path, importsOwner, nameUsages)
                    path.parent is MvPathType -> putUsage(path, importsOwner, typeUsages)
                    else -> putUsage(path, importsOwner, nameUsages)
                }
                true
            }
        }
        PathUsages(
            ScopePathUsages(mainNameUsages, mainTypeUsages),
            ScopePathUsages(testNameUsages, testTypeUsages),
        )
    }
}

private fun putUsage(element: MvPath, currentImportsOwner: MvImportsOwner, itemUsages: ItemUsages) {
    for (ancestor in element.ancestorsOfType<MvImportsOwner>()) {
        if (ancestor == currentImportsOwner) break

        val imports = ancestor.importInfo

    }

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
