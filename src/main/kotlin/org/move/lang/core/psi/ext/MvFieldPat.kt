package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvPolyVariantReference


val MvFieldPat.structPat: MvStructPat
    get() = ancestorStrict()!!

val MvFieldPat.pat: MvPat? get() =
    this.bindingPat ?: this.fieldPatBinding?.pat

val MvFieldPat.isShorthand: Boolean get() = this.fieldPatBinding == null

val MvFieldPat.kind: PatFieldKind
    get() = bindingPat?.let { PatFieldKind.Shorthand(it) }
        ?: PatFieldKind.Full(this.identifier!!, this.fieldPatBinding?.pat!!)

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

abstract class MvFieldPatMixin(node: ASTNode): MvElementImpl(node),
                                               MvFieldPat {
    override val referenceNameElement: PsiElement
        get() {
            val bindingPat = this.bindingPat
            if (bindingPat != null) {
                return bindingPat.identifier
            } else {
                return this.identifier
            }
        }

    override fun getReference(): MvPolyVariantReference =
        MvFieldReferenceImpl(this, shorthand = this.isShorthand)
}
