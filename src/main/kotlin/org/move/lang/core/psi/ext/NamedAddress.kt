package org.move.lang.core.psi.ext

import org.move.cli.GlobalScope
import org.move.lang.core.psi.MoveNamedAddress
import org.move.lang.getCorrespondingMoveToml

fun MoveNamedAddress.assignedValue(scope: GlobalScope): String? {
    val moveToml = this.containingFile?.getCorrespondingMoveToml() ?: return null
    return moveToml
        .getAddresses(scope)[this.text]
}
