package org.move.ide.hints.type

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.codeInsight.hints.declarative.CollapseState.Collapsed
import com.intellij.codeInsight.hints.declarative.CollapseState.Expanded
import com.intellij.psi.createSmartPointer
import org.move.ide.presentation.hintText
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.core.types.ty.TyTuple
import org.move.lang.core.types.ty.TyVector

object MvTypeHintsFactory {
    fun typeHint(type: Ty, treeBuilder: PresentationTreeBuilder) {
        treeBuilder.typeHint(0, type)
    }

    //    private const val UNNAMED_MARK = "<unnamed>"
    private const val ANONYMOUS_MARK = "<anonymous>"

    private fun PresentationTreeBuilder.typeHint(level: Int, type: Ty) {
        when (type) {
            is TyTuple -> tupleTypeHint(level, type)
            is TyAdt -> adtTypeHint(level, type)
            is TyVector -> vectorTypeHint(level, type)
            else -> {
                text(type.hintText())
            }
        }
    }

    private fun PresentationTreeBuilder.adtTypeHint(level: Int, tyAdt: TyAdt) {
        val itemName = tyAdt.item.name ?: ANONYMOUS_MARK
        text(
            itemName,
            InlayActionData(
                PsiPointerInlayActionPayload(tyAdt.item.createSmartPointer()),
                PsiPointerInlayActionNavigationHandler.HANDLER_ID
            )
        )
        if (tyAdt.typeArguments.isEmpty()) return
        collapsibleList(
            state = calcCollapseState(level, tyAdt),
            expandedState = {
                toggleButton { text("<") }
                join(
                    tyAdt.typeArguments,
                    op = {
                        typeHint(level + 1, it)
                    },
                    separator = { text(", ") }
                )
                toggleButton { text(">") }
            },
            collapsedState = {
                toggleButton { text("<...>") }
            })
    }

    private fun PresentationTreeBuilder.tupleTypeHint(level: Int, tyTuple: TyTuple) {
        collapsibleList(
            state = calcCollapseState(level, tyTuple),
            expandedState = {
                toggleButton { text("(") }
                join(
                    tyTuple.types,
                    op = {
                        typeHint(level + 1, it)
                    },
                    separator = { text(", ") }
                )
                toggleButton { text(")") }
            },
            collapsedState = {
                toggleButton { text("(...)") }
            })
    }

    private fun PresentationTreeBuilder.vectorTypeHint(level: Int, tyVector: TyVector) {
        text("vector")
        collapsibleList(
            state = calcCollapseState(level, tyVector),
            expandedState = {
                toggleButton { text("[") }
                typeHint(level + 1, tyVector.item)
                toggleButton { text("]") }
            },
            collapsedState = {
                toggleButton { text("[...]") }
            })
    }

    private fun <T> PresentationTreeBuilder.join(
        elements: List<T>,
        op: PresentationTreeBuilder.(T) -> Unit,
        separator: PresentationTreeBuilder.() -> Unit
    ) {
        var isFirst = true
        for (element in elements) {
            if (isFirst) {
                isFirst = false
            } else {
                separator()
            }
            op(this, element)
        }
    }

    private const val COLLAPSE_FROM_LEVEL: Int = 2
    private const val TY_ADT_NAME_LENGTH_COLLAPSE_LIMIT: Int = 10

    private fun calcCollapseState(level: Int, ty: Ty): CollapseState {
        when (ty) {
            is TyAdt -> {
                // check whether the name is too long, collapse the type arguments if so
                val itemName = ty.item.name
                if (itemName != null) {
                    if (itemName.length > TY_ADT_NAME_LENGTH_COLLAPSE_LIMIT) {
                        return Collapsed
                    }
                }
            }
        }
        return if (level < COLLAPSE_FROM_LEVEL) Expanded else Collapsed
    }
}