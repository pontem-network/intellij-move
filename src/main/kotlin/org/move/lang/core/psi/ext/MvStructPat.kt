package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvStructPat
import org.move.lang.core.psi.MvStructPatField

val MvStructPat.patFields: List<MvStructPatField>
    get() =
        structPatFieldsBlock.structPatFieldList

val MvStructPat.patFieldNames: List<String>
    get() =
        patFields.map { it.referenceName }

val MvStructPat.structItem: MvStruct? get() = this.path.reference?.resolveFollowingAliases() as? MvStruct
