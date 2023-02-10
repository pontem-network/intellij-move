package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvModuleUseSpeck
import org.move.lang.core.psi.MvUseAlias
import org.move.lang.core.psi.MvUseItem
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

val MvUseAlias.useItem: MvUseItem? get() = this.parent as? MvUseItem

val MvUseAlias.moduleUseSpeck: MvModuleUseSpeck? get() = this.parent as? MvModuleUseSpeck

abstract class MvUseAliasMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                MvUseAlias
