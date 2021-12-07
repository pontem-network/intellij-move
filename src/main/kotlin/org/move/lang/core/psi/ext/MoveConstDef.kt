package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MvIcons
import org.move.lang.core.psi.MvConstDef
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferMvTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

val MvConstDef.declaredTy: Ty
    get() = this.typeAnnotation?.type?.let { inferMvTypeTy(it) } ?: TyUnknown
