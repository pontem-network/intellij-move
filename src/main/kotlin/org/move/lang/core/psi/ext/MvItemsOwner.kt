package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ScopeEntryWithVisibility
import org.move.lang.core.resolve.asEntry
import org.move.stdext.buildList

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

val MvModule.globalVariableEntries: List<ScopeEntryWithVisibility>
    get() {
        val module = this
        return buildList {
            addAll(
                module.allModuleSpecs()
                       .map {
                           it.moduleItemSpecs()
                               .flatMap { spec -> spec.itemSpecBlock?.globalVariables().orEmpty().mapNotNull { it.asEntry() } }
                       }
                       .flatten())
        }
    }

val MvItemsOwner.firstItem: MvElement?
    get() = items().firstOrNull { it !is MvAttr }
