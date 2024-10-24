package org.move.ide.hints.type

import com.intellij.codeInsight.hints.declarative.CollapseState.Collapsed
import com.intellij.codeInsight.hints.declarative.CollapseState.Expanded
import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder
import org.move.ide.presentation.hintText
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.core.types.ty.TyTuple
import org.move.lang.core.types.ty.TyVector

object MvTypeHintsFactory {
    private const val COLLAPSE_FROM_LEVEL: Int = 2
//
//    private const val CAPTURE_OF = "capture of "
//    private const val UNNAMED_MARK = "<unnamed>"
    private const val ANONYMOUS_MARK = "<anonymous>"

    fun typeHint(type: Ty, treeBuilder: PresentationTreeBuilder) {
        treeBuilder.typeHint(0, type)
    }

    private fun PresentationTreeBuilder.typeHint(level: Int, type: Ty) {
        when (type) {
            is TyTuple -> tupleTypeHint(level, type)
            is TyAdt -> adtTypeHint(level, type)
            is TyVector -> vectorTypeHint(level, type)
//            is PsiClassType -> classTypeHint(level, type)
//            is PsiDisjunctionType -> {
//                join(
//                    type.disjunctions,
//                    op = {
//                        typeHint(level, it)
//                    },
//                    separator = {
//                        text(" | ")
//                    }
//                )
//            }
//            is PsiIntersectionType -> {
//                join(
//                    type.conjuncts,
//                    op = {
//                        typeHint(level, it)
//                    },
//                    separator = {
//                        text(" & ")
//                    }
//                )
//            }
            else -> {
                text(type.hintText())
            }
        }
    }

    private fun PresentationTreeBuilder.adtTypeHint(level: Int, tyAdt: TyAdt) {
        val itemName = tyAdt.item.name ?: ANONYMOUS_MARK
        text(itemName)
        if (tyAdt.typeArguments.isEmpty()) return
        collapsibleList(
            state = if (level < COLLAPSE_FROM_LEVEL) Expanded else Collapsed,
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
            state = if (level < COLLAPSE_FROM_LEVEL) Expanded else Collapsed,
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
            state = if (level < COLLAPSE_FROM_LEVEL) Expanded else Collapsed,
            expandedState = {
                toggleButton { text("[") }
                typeHint(level + 1, tyVector.item)
                toggleButton { text("]") }
            },
            collapsedState = {
                toggleButton { text("[...]") }
            })
    }

//    private fun PresentationTreeBuilder.classTypeHint(level: Int, classType: PsiClassType) {
//        val aClass = classType.resolve()
//
//        val className = classType.className ?: ANONYMOUS_MARK // TODO here it may be not exactly true, the class might be unresolved
//        text(className, aClass?.qualifiedName?.let { InlayActionData(StringInlayActionPayload(it), JavaFqnDeclarativeInlayActionHandler.HANDLER_NAME) })
//        if (classType.parameterCount == 0) return
//        collapsibleList(expandedState = {
//            toggleButton {
//                text("<")
//            }
//            join(
//                classType.parameters,
//                op = {
//                    typeHint(level + 1, it)
//                },
//                separator = {
//                    text(", ")
//                }
//            )
//            toggleButton {
//                text(">")
//            }
//        }, collapsedState = {
//            toggleButton {
//                text("<...>")
//            }
//        })
//    }

//    private fun <T> PresentationTreeBuilder.join(
//        elements: Array<T>,
//        op: PresentationTreeBuilder.(T) -> Unit,
//        separator: PresentationTreeBuilder.() -> Unit
//    ) {
//        var isFirst = true
//        for (element in elements) {
//            if (isFirst) {
//                isFirst = false
//            } else {
//                separator()
//            }
//            op(this, element)
//        }
//    }

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
}