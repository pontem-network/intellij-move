package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvType

interface MvTypeAscriptionOwner: MvElement {
    val type: MvType?
}