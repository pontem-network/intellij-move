package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import org.move.ide.presentation.expectedBindingFormText
import org.move.ide.presentation.name
import org.move.ide.presentation.text
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvReturnExpr
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.ty.Ability
import org.move.lang.core.types.ty.Ty

enum class TypeErrorScope {
    MAIN, MODULE;
}

sealed class TypeError(open val element: PsiElement) {
    abstract fun message(): String

    companion object {
        fun isAllowedTypeError(error: TypeError, typeErrorScope: TypeErrorScope): Boolean {
            return when (typeErrorScope) {
                TypeErrorScope.MODULE -> error is CircularType
                TypeErrorScope.MAIN -> {
                    if (error is CircularType) return false
                    val element = error.element
                    if (
                        (error is UnsupportedBinaryOp || error is IncompatibleArgumentsToBinaryExpr)
                        && (element is MvElement && element.isMsl())
                    ) {
                        return false
                    }
                    true
                }
            }
        }
    }

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
    }

    data class AbilitiesMismatch(
        override val element: PsiElement,
        val elementTy: Ty,
        val missingAbilities: Set<Ability>
    ) : TypeError(element) {
        override fun message(): String {
            return "The type '${elementTy.text()}' " +
                    "does not have required ability '${missingAbilities.map { it.label() }.first()}'"
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
    }

    data class InvalidUnpacking(
        override val element: PsiElement,
        val assignedTy: Ty,
    ) : TypeError(element) {
        override fun message(): String {
            return "Invalid unpacking. Expected ${assignedTy.expectedBindingFormText()}"
        }
    }

    data class CircularType(
        override val element: PsiElement,
        val structItem: MvStruct
    ) : TypeError(element) {
        override fun message(): String {
            return "Circular reference of type '${structItem.name}'"
        }
    }
}
