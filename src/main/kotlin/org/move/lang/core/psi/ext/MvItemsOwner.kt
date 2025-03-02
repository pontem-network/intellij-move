package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.resolve.scopeEntry.asEntries

interface MvItemsOwner: MvElement {
    val useStmtList: List<MvUseStmt>
}

fun MvItemsOwner.items(): Sequence<MvElement> {
    val startChild = when (this) {
        is MvModule -> this.lBrace
        is MvScript -> this.lBrace
        else -> this.firstChild
    }
    return generateSequence(startChild) { it.nextSibling }.filterIsInstance<MvElement>()
}

val MvModuleItemSpec.globalVariableEntries: List<ScopeEntry>
    get() {
        return this.itemSpecBlock?.globalVariables().orEmpty().asEntries()
    }

val MvModule.globalVariableEntries: List<ScopeEntry>
    get() {
        return this.moduleItemSpecList.flatMap { it.globalVariableEntries }
    }

val MvItemsOwner.firstItem: MvElement?
    get() = items().firstOrNull { it !is MvAttr }
