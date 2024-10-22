package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.lang.core.psi.*

fun MvSpecCodeBlock.builtinSpecConsts(): List<MvConst> {
    return getProjectPsiDependentCache(this) {
        val builtinsModule = it.project.psiFactory.module("""
            module 0x0::spec_builtins {
                const MAX_U8: u8 = 255;
                const MAX_U16: u16 = 65535;
                const MAX_U32: u32 = 4294967295;
                const MAX_U64: u64 = 18446744073709551615;
                const MAX_U128: u128 = 340282366920938463463374607431768211455;
                const MAX_U256: u256 = 115792089237316195423570985008687907853269984665640564039457584007913129639935;
            }            
        """.trimIndent())
        builtinsModule.constList
    }
}

abstract class MvSpecCodeBlockMixin(node: ASTNode) : MvElementImpl(node),
                                                     MvSpecCodeBlock {
    override val useStmtList: List<MvUseStmt>
        get() = this.stmtList.filterIsInstance<MvUseStmt>()
}
