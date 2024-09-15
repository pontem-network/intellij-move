package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvPatRest
import org.move.lang.core.psi.MvPatTupleStruct

val MvPatTupleStruct.patRest: MvPatRest? get() = patList.firstOrNull { it is MvPatRest } as? MvPatRest