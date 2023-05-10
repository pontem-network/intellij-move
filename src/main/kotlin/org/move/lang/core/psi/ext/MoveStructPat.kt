package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStructPat
import org.move.lang.core.psi.MvStructPatField

val MvStructPat.patFields: List<MvStructPatField>
    get() =
        structPatFieldsBlock.structPatFieldList

val MvStructPat.patFieldNames: List<String>
    get() =
        patFields.map { it.referenceName }
