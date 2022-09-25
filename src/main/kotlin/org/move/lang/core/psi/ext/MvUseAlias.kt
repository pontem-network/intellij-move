package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvModuleUseSpeck
import org.move.lang.core.psi.MvUseAlias
import org.move.lang.core.psi.MvUseItem

val MvUseAlias.useItem: MvUseItem? get() = this.parent as? MvUseItem

val MvUseAlias.moduleUseSpeck: MvModuleUseSpeck? get() = this.parent as? MvModuleUseSpeck
