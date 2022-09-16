package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.core.MOVE_BINARY_OPS
import org.move.lang.core.psi.MvBinaryOp

val MvBinaryOp.operator: PsiElement
    get() = requireNotNull(node.findChildByType(MOVE_BINARY_OPS)) { "guaranteed to be not-null by parser" }.psi

val MvBinaryOp.op: String get() = operator.text
