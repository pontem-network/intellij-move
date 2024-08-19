package org.move.lang.core.psi.ext

import com.intellij.psi.PsiComment
import org.move.lang.core.psi.*
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
    return generateSequence(startChild) { it.nextSibling }
        .filterIsInstance<MvElement>()
//        .filter { it !is MvAttr }
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

val MvItemsOwner.firstItem: MvElement?
    get() = items().firstOrNull { it !is MvAttr }
