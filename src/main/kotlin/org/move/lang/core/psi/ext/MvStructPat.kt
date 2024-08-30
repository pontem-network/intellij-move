package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvPatStruct

val MvPatStruct.fieldNames: Set<String>
    get() =
        patFieldList.map { it.fieldReferenceName }.toSet()

//val MvPatStruct.structItem: MvStruct? get() = this.path.reference?.resolveFollowingAliases() as? MvStruct
