package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvAttrItem
import org.move.lang.core.psi.MvAttrItemArgument
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvRefExpr

fun MvRefExpr.isErrorConst(): Boolean {
    val itemArgument = this.parent as? MvAttrItemArgument ?: return false
    if (itemArgument.identifier.text != "abort_code") return false

    val attrItem = itemArgument.parent.parent as? MvAttrItem ?: return false
    return (attrItem.attr.owner as? MvFunction)?.isTest
        ?: false
}
