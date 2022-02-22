package org.move.lang.core.psi.ext

import org.move.lang.MvElementTypes.POST
import org.move.lang.core.psi.MvLetStatement
import org.move.lang.core.types.ty.Ty

val MvLetStatement.isPost: Boolean get() = this.hasChild(POST)

val MvLetStatement.declaredTy: Ty? get() = this.typeAnnotation?.type?.ty()
