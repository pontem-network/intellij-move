package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStructPat
import org.move.lang.core.psi.MvStructPatField

val MvStructPat.fields: List<MvStructPatField>
    get() =
        structPatFieldsBlock.structPatFieldList

val MvStructPat.fieldNames: List<String>
    get() =
        fields.map { it.referenceName }
