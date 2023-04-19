package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvItemSpecBlock
import org.move.lang.core.psi.MvUseStmt

abstract class MvItemSpecBlockMixin(node: ASTNode) : MvElementImpl(node),
                                                     MvItemSpecBlock {
    override val useStmtList: List<MvUseStmt>
        get() = this.stmtList.filterIsInstance<MvUseStmt>()
}
