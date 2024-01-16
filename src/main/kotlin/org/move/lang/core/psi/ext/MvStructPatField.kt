package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvStructRefFieldReferenceImpl
import org.move.lang.core.resolve.ref.MvStructPatShorthandFieldReferenceImpl

val MvStructPatField.structPat: MvStructPat
    get() = ancestorStrict()!!

val MvStructPatField.pat: MvPat?
    get() {
        return this.bindingPat ?: this.structPatFieldBinding?.pat
    }

val MvStructPatField.isShorthand: Boolean get() = this.structPatFieldBinding == null

val MvStructPatField.kind: PatFieldKind
    get() = bindingPat?.let { PatFieldKind.Shorthand(it) }
        ?: PatFieldKind.Full(this.identifier!!, this.structPatFieldBinding?.pat!!)

// PatField ::= identifier ':' Pat | box? PatBinding
sealed class PatFieldKind {
    /**
     * struct S { a: i32 }
     * let S { a : ref b } = ...
     *         ~~~~~~~~~
     */
    data class Full(val ident: PsiElement, val pat: MvPat): PatFieldKind()
    /**
     * struct S { a: i32 }
     * let S { ref a } = ...
     *         ~~~~~
     */
    data class Shorthand(val binding: MvBindingPat): PatFieldKind()
}

val PatFieldKind.fieldName: String
    get() = when (this) {
        is PatFieldKind.Full -> ident.text
        is PatFieldKind.Shorthand -> binding.name
    }


abstract class MvStructPatFieldMixin(node: ASTNode) : MvElementImpl(node),
                                                      MvStructPatField {
    override val referenceNameElement: PsiElement
        get() {
            val bindingPat = this.bindingPat
            if (bindingPat != null) {
                return bindingPat.identifier
            } else {
                return this.identifier
            }
        }

    override fun getReference(): MvPolyVariantReference {
        return if (this.isShorthand) {
            MvStructPatShorthandFieldReferenceImpl(this)
        } else {
            MvStructRefFieldReferenceImpl(this)
        }
    }
}
