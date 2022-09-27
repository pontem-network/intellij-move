package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvStructPat

val MvStructPat.struct: MvStruct? get() = this.path.reference?.resolve() as? MvStruct
