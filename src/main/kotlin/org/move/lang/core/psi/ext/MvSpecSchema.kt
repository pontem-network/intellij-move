package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvSpecSchema

val MvSpecSchema.typeParams get() = typeParameterList?.typeParameterList.orEmpty()

val MvSpecSchema.declaredVars get() = this.specBlock?.schemaVars().orEmpty()
