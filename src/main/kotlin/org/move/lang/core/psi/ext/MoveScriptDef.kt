package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveImportStatement
import org.move.lang.core.psi.MoveScriptDef

abstract class MoveScriptDefMixin(node: ASTNode) : MoveElementImpl(node),
                                                   MoveScriptDef {
    override val importStatements: List<MoveImportStatement>
        get() =
            this.scriptBlock?.importStatementList.orEmpty()
}