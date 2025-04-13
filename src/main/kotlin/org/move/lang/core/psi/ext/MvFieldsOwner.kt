package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

interface MvFieldsOwner: MvNameIdentifierOwner {
    val blockFields: MvBlockFields?
    val tupleFields: MvTupleFields?
}

val MvFieldsOwner.itemElement: MvStructOrEnumItemElement
    get() = when (this) {
        is MvStruct -> this
        is MvEnumVariant -> this.enumItem
        else -> error("exhaustive")
    }

val MvFieldsOwner.fields: List<MvFieldDecl>
    get() = namedFields + positionalFields

val MvFieldsOwner.namedFields: List<MvNamedFieldDecl>
    get() = blockFields?.namedFieldDeclList.orEmpty()

val MvFieldsOwner.fieldNames: List<String> get() = namedFields.map { it.name }

val MvFieldsOwner.positionalFields: List<MvTupleFieldDecl>
    get() = tupleFields?.tupleFieldDeclList.orEmpty()

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