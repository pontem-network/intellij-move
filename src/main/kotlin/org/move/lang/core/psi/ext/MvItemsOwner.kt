package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.stdext.buildList

interface MvItemsOwner: MvElement {
    val useStmtList: List<MvUseStmt> get() = emptyList()
}

fun MvItemsOwner.items(): Sequence<MvElement> {
    return generateSequence(firstChild) { it.nextSibling }
        .filterIsInstance<MvElement>()
        .filter { it !is MvAttr }
}

val MvItemsOwner.itemElements: List<MvItemElement>
    get() {
        return this.items().filterIsInstance<MvItemElement>().toList()
    }

val MvModule.innerSpecItems: List<MvItemElement>
    get() {
        val module = this
        return buildList {
            addAll(module.allModuleSpecs()
                       .map {
                           it.moduleItemSpecs()
                               .flatMap { spec -> spec.itemSpecBlock?.globalVariables().orEmpty() }
                       }
                       .flatten())
            addAll(module.specInlineFunctions())
        }
    }

fun MvItemsOwner.allUseItems(): List<MvNamedElement> = emptyList()