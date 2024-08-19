package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvPatStruct
import org.move.lang.core.psi.MvStruct

val MvPatStruct.providedFieldNames: Set<String>
    get() =
        patFieldList.map { it.fieldReferenceName }.toSet()

//val MvPatStruct.structItem: MvStruct? get() = this.path.reference?.resolveFollowingAliases() as? MvStruct
