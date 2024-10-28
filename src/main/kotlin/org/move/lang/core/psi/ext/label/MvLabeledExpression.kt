package org.move.lang.core.psi.ext.label

import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvLabelDecl

interface MvLabeledExpression: MvElement {
    val labelDecl: MvLabelDecl?
}