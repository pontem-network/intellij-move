package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvSchema

val MvSchema.typeParams get() = typeParameterList?.typeParameterList.orEmpty()

val MvSchema.fieldStmts get() = this.specBlock?.schemaFields().orEmpty()
