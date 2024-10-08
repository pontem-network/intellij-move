package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvAttr
import org.move.lang.core.psi.MvAttrItem
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvPathExpr

fun MvPathExpr.isAbortCodeConst(): Boolean {
    val abortCodeItem =
        (this.parent.parent as? MvAttrItem)
            ?.takeIf { it.isAbortCode }
            ?: return false
    val attr = abortCodeItem.ancestorStrict<MvAttr>() ?: return false
    return (attr.attributeOwner as? MvFunction)?.hasTestAttr ?: false
}
