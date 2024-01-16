package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvStructRefFieldReferenceImpl
import org.move.lang.core.resolve.ref.MvStructLitShorthandFieldReferenceImpl

val MvStructLitField.structLitExpr: MvStructLitExpr
    get() = ancestorStrict()!!

val MvStructLitField.isShorthand: Boolean
    get() = !hasChild(MvElementTypes.COLON)

inline fun <reified T : MvElement> MvStructLitField.resolveToElement(): T? =
    reference.multiResolve().filterIsInstance<T>().singleOrNull()

fun MvStructLitField.resolveToDeclaration(): MvStructField? = resolveToElement()
fun MvStructLitField.resolveToBinding(): MvBindingPat? = resolveToElement()

abstract class MvStructLitFieldMixin(node: ASTNode) : MvElementImpl(node),
                                                      MvStructLitField {
    override fun getReference(): MvPolyVariantReference {
        if (!this.isShorthand) return MvStructRefFieldReferenceImpl(this)
        return MvStructLitShorthandFieldReferenceImpl(this)
    }
}
