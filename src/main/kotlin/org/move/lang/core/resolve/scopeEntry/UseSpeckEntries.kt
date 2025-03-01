package org.move.lang.core.resolve.scopeEntry

import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.ide.inspections.imports.usageScope
import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.MvUseAlias
import org.move.lang.core.psi.MvUseSpeck
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.psi.ext.MvItemsOwner
import org.move.lang.core.psi.ext.childOfType
import org.move.lang.core.psi.ext.childrenOfType
import org.move.lang.core.psi.ext.qualifier
import org.move.lang.core.resolve.ref.ALL_NS
import org.move.lang.core.resolve.ref.itemNs

val MvItemsOwner.useSpeckEntries: List<ScopeEntry> get() {
    return getProjectPsiDependentCache(this, MvItemsOwner::useSpeckEntriesInner)
}

private fun MvItemsOwner.useSpeckEntriesInner(): List<ScopeEntry> {
    val speckItems = buildList {
        for (useStmt in useStmtList) {
            val usageScope = useStmt.usageScope
            useStmt.forEachLeafSpeck { speckPath, alias ->
                add(UseSpeckItem(speckPath, alias, usageScope))
            }
        }
    }
    return buildList {
        for (speckItem in speckItems) {
            val speckPath = speckItem.speckPath
            val alias = speckItem.alias

            val resolvedItem = speckPath.reference?.resolve()
            if (resolvedItem == null) {
                // aliased element cannot be resolved, but alias itself is valid, resolve to it
                if (alias != null) {
                    val referenceName = speckItem.aliasOrSpeckName ?: continue
                    // any ns is possible here
                    add(
                        ScopeEntry(
                            referenceName,
                            alias,
                            ALL_NS
                        )
                    )
                }
                continue
            }
            val element = alias ?: resolvedItem
            val itemNs = resolvedItem.itemNs
            val speckItemName = speckItem.aliasOrSpeckName ?: continue
            add(
                ScopeEntry(
                    speckItemName,
                    element,
                    itemNs,
                    customItemScope = speckItem.stmtUsageScope,
                )
            )
        }
    }
}

private data class UseSpeckItem(
    val speckPath: MvPath,
    val alias: MvUseAlias?,
    val stmtUsageScope: NamedItemScope
) {
    val aliasOrSpeckName: String?
        get() {
            if (alias != null) {
                return alias.name
            } else {
                var n = speckPath.referenceName ?: return null
                // 0x1::m::Self -> 0x1::m
                if (n == "Self") {
                    n = speckPath.qualifier?.referenceName ?: return null
                }
                return n
            }
        }
}

fun interface LeafUseSpeckConsumer {
    fun consume(path: MvPath, useAlias: MvUseAlias?): Boolean
}

fun MvUseStmt.forEachLeafSpeck(consumer: LeafUseSpeckConsumer) {
    val rootUseSpeck = this.childOfType<MvUseSpeck>() ?: return
    val useGroup = rootUseSpeck.useGroup
    if (useGroup == null) {
        // basePath is null, path is full path of useSpeck
        val alias = rootUseSpeck.useAlias
        if (!consumer.consume(rootUseSpeck.path, alias)) return
    } else {
        for (childSpeck in useGroup.childrenOfType<MvUseSpeck>()) {
            val alias = childSpeck.useAlias
            if (!consumer.consume(childSpeck.path, alias)) continue
        }
    }
}
