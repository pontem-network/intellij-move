package org.move.lang.core.psi.ext

import org.move.lang.MvElementTypes.POST
import org.move.lang.core.psi.MvLetStmt
import org.move.lang.core.types.ty.Ty

val MvLetStmt.isPost: Boolean get() = this.hasChild(POST)

val MvLetStmt.declaredTy: Ty? get() = this.typeAnnotation?.type?.ty()
