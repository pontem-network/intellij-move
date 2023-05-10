package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvStructPat

val MvStructPat.structItem: MvStruct? get() = this.path.reference?.resolveWithAliases() as? MvStruct
