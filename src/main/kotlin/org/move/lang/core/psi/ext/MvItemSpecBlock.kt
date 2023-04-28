package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValuesManager
import org.move.lang.core.psi.*

fun MvSpecCodeBlock.builtinSpecConsts(): List<MvConst> {
    return CachedValuesManager.getProjectPsiDependentCache(this) {
        val psiFactory = it.project.psiFactory
        listOf(
            psiFactory.builtinConst(
                "const MAX_U256: u256 = " +
                        "115792089237316195423570985008687907853269984665640564039457584007913129639935;"
            ),
            psiFactory.builtinConst("const MAX_U128: num = 340282366920938463463374607431768211455;"),
            psiFactory.builtinConst("const MAX_U64: num = 18446744073709551615;"),
            psiFactory.builtinConst("const MAX_U32: num = 4294967295;"),
            psiFactory.builtinConst("const MAX_U16: num = 65535;"),
            psiFactory.builtinConst("const MAX_U8: num = 255;"),
        )
    }
}

abstract class MvSpecCodeBlockMixin(node: ASTNode) : MvElementImpl(node),
                                                     MvSpecCodeBlock {
    override val useStmtList: List<MvUseStmt>
        get() = this.stmtList.filterIsInstance<MvUseStmt>()
}
