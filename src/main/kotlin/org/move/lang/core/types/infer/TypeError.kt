package org.move.lang.core.types.infer

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.move.ide.inspections.fixes.IntegerCastFix
import org.move.ide.presentation.name
import org.move.ide.presentation.text
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.ty.*

sealed class TypeError(open val element: PsiElement) : TypeFoldable<TypeError> {
    abstract fun message(): String

    override fun innerVisitWith(visitor: TypeVisitor): Boolean = true

    open fun range(): TextRange = element.textRange

    open fun fix(): LocalQuickFix? = null

    data class TypeMismatch(
        override val element: PsiElement,
        val expectedTy: Ty,
        val actualTy: Ty
    ) : TypeError(element) {
        override fun message(): String {
            return when (element) {
                is MvReturnExpr -> "Invalid return type '${actualTy.name()}', expected '${expectedTy.name()}'"
                else -> "Incompatible type '${actualTy.name()}', expected '${expectedTy.name()}'"
            }
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError {
            return TypeMismatch(element, folder(expectedTy), folder(actualTy))
        }

        override fun fix(): LocalQuickFix? {
            if (element is MvExpr) {
                if (expectedTy is TyInteger && actualTy is TyInteger
                    && !expectedTy.isDefault() && !actualTy.isDefault()
                ) {
                    if (this.element.isMsl()) return null

                    val expr = element
                    val inference = expr.inference(false) ?: return null

                    if (expr is MvParensExpr && expr.expr is MvCastExpr) {
                        val castExpr = expr.expr as MvCastExpr
                        val originalTy = inference.getExprType(castExpr.expr) as? TyInteger ?: return null
                        if (originalTy == expectedTy) {
                            return IntegerCastFix.RemoveCast(castExpr)
                        } else {
                            return IntegerCastFix.ChangeCast(castExpr, expectedTy)
                        }
                    }
                    return IntegerCastFix.AddCast(element, expectedTy)
                }
            }
            return null
        }
    }

    data class UnsupportedBinaryOp(
        override val element: PsiElement,
        val ty: Ty,
        val op: String
    ) : TypeError(element) {
        override fun message(): String {
            return "Invalid argument to '$op': " +
                    "expected integer type, but found '${ty.text()}'"
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError {
            return UnsupportedBinaryOp(element, folder(ty), op)
        }
    }

    data class IncompatibleArgumentsToBinaryExpr(
        override val element: PsiElement,
        val leftTy: Ty,
        val rightTy: Ty,
        val op: String,
    ) : TypeError(element) {
        override fun message(): String {
            return "Incompatible arguments to '$op': " +
                    "'${leftTy.text()}' and '${rightTy.text()}'"
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError {
            return IncompatibleArgumentsToBinaryExpr(element, folder(leftTy), folder(rightTy), op)
        }
    }

    data class InvalidUnpacking(
        override val element: PsiElement,
        val assignedTy: Ty,
    ) : TypeError(element) {
        override fun message(): String {
            return when {
                element is MvPatStruct &&
                        (assignedTy !is TyAdt && assignedTy !is TyTuple) -> {
                    "Assigned expr of type '${assignedTy.text(fq = false)}' " +
                            "cannot be unpacked with struct pattern"
                }
                element is MvPatTuple &&
                        (assignedTy !is TyAdt && assignedTy !is TyTuple) -> {
                    "Assigned expr of type '${assignedTy.text(fq = false)}' " +
                            "cannot be unpacked with tuple pattern"
                }
                else -> "Invalid unpacking. Expected ${assignedTy.assignedTyFormText()}"
            }
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError {
            return InvalidUnpacking(element, folder(assignedTy))
        }

        private fun Ty.assignedTyFormText(): String {
            return when (this) {
                is TyTuple -> {
                    val expectedForm = this.types.joinToString(", ", "(", ")") { "_" }
                    "tuple binding of length ${this.types.size}: $expectedForm"
                }
                is TyAdt -> "struct binding of type ${this.text(true)}"
                else -> "a single variable"
            }
        }
    }

    data class CircularType(
        override val element: PsiElement,
        val structItem: MvStruct
    ) : TypeError(element) {
        override fun message(): String {
            return "Circular reference of type '${structItem.name}'"
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError = this
    }

    data class ExpectedNonReferenceType(
        override val element: PsiElement,
        val actualTy: Ty,
    ) : TypeError(element) {

        override fun message(): String {
            return "Expected a single non-reference type, but found: '${actualTy.text(fq = false)}'"
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError {
            return ExpectedNonReferenceType(element, folder.fold(actualTy))
        }
    }

    data class InvalidDereference(
        override val element: PsiElement,
        val actualTy: Ty
    ): TypeError(element) {
        override fun message(): String {
            return "Invalid dereference. Expected '&_' but found '${actualTy.text(fq = false)}'"
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError {
            return InvalidDereference(element, folder.fold(actualTy))
        }
    }

    data class IndexingIsNotAllowed(
        override val element: PsiElement,
        val actualTy: Ty,
    ): TypeError(element) {
        override fun message(): String {
            return "Indexing receiver type should be vector or resource, got '${actualTy.text(fq = false)}'"
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError {
            return IndexingIsNotAllowed(element, folder.fold(actualTy))
        }
    }
}
