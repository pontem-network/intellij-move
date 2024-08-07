package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvStructPat

val MvStructPat.patFieldNames: List<String>
    get() =
        fieldPatList.map { it.referenceName }

val MvStructPat.structItem: MvStruct? get() = this.path.reference?.resolveFollowingAliases() as? MvStruct
