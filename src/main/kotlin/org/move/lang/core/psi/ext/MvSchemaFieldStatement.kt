package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvSchemaFieldStmt
import org.move.lang.core.types.infer.inferMvTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

//abstract class MvSchemaFieldStmtMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
//                                                            MvSchemaFieldStmt

fun MvSchemaFieldStmt.declaredTy(msl: Boolean): Ty =
    this.typeAnnotation.type?.let { inferMvTypeTy(it, msl) } ?: TyUnknown
