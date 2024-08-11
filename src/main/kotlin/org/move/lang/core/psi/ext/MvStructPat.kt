package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvStructPat

val MvStructPat.providedFieldNames: Set<String>
    get() =
        fieldPatList.map { it.fieldReferenceName }.toSet()

val MvStructPat.structItem: MvStruct? get() = this.path.reference?.resolveFollowingAliases() as? MvStruct
