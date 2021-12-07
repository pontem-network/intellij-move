package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvImportAlias
import org.move.lang.core.psi.MvModuleImport
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

abstract class MvImportAliasMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                     MvImportAlias {
}
