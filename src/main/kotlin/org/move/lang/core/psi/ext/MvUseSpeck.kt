package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.MvUseGroup
import org.move.lang.core.psi.MvUseSpeck

val MvUseSpeck.qualifier: MvPath? get() {
    val parentUseSpeck = (context as? MvUseGroup)?.parentUseSpeck ?: return null
    return parentUseSpeck.pathOrQualifier
}
val MvUseSpeck.pathOrQualifier: MvPath? get() = path ?: qualifier