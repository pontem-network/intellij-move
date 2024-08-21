package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvPatBinding
import org.move.lang.core.psi.MvPatField
import org.move.lang.core.psi.MvPat
import org.move.lang.core.psi.MvPatStruct


val MvPatField.patStruct: MvPatStruct get() = ancestorStrict()!!

val MvPatField.fieldReferenceName: String
    get() = if (this.patFieldFull != null) {
        this.patFieldFull!!.referenceName
    } else {
        this.patBinding!!.referenceName
    }


//val MvFieldPat.pat: MvPat?
//    get() =
//        this.bindingPat ?: this.fieldPatBinding?.pat

//val MvFieldPat.isShorthand: Boolean get() = kind is PatFieldKind.Shorthand

val MvPatField.kind: PatFieldKind
    get() = patBinding?.let { PatFieldKind.Shorthand(it) }
        ?: PatFieldKind.Full(patFieldFull!!.referenceNameElement, patFieldFull!!.pat)

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
    data class Shorthand(val binding: MvPatBinding): PatFieldKind()
}

val PatFieldKind.fieldName: String
    get() = when (this) {
        is PatFieldKind.Full -> ident.text
        is PatFieldKind.Shorthand -> binding.name
    }
