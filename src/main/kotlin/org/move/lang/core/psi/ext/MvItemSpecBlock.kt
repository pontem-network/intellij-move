package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.lang.core.psi.*

abstract class MvSpecCodeBlockMixin(node: ASTNode) : MvElementImpl(node),
                                                     MvSpecCodeBlock {
    override val useStmtList: List<MvUseStmt>
        get() = this.stmtList.filterIsInstance<MvUseStmt>()
}
