package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvBlockFields
import org.move.lang.core.psi.MvNameIdentifierOwner
import org.move.lang.core.psi.MvNamedFieldDecl

interface MvFieldsOwner: MvNameIdentifierOwner {
    val blockFields: MvBlockFields?
}

val MvFieldsOwner.fields: List<MvNamedFieldDecl>
    get() = namedFields //+ positionalFields

val MvFieldsOwner.namedFields: List<MvNamedFieldDecl>
    get() = blockFields?.namedFieldDeclList.orEmpty()


/**
 * True for:
 * ```
 * struct S;
 * enum E { A }
 * ```
 * but false for:
 * ```
 * struct S {}
 * struct S();
 * ```
 */
val MvFieldsOwner.isFieldless: Boolean
    get() = blockFields == null //&& tupleFields == null