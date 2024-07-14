package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvUseGroup
import org.move.lang.core.psi.MvUseSpeck

val MvUseGroup.parentUseSpeck: MvUseSpeck get() = parent as MvUseSpeck