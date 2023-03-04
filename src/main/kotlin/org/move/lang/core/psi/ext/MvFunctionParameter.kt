package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvFunctionLike
import org.move.lang.core.psi.MvFunctionParameter

val MvFunctionParameter.functionLike get() = this.parent.parent as? MvFunctionLike

// BindingPat has required name
val MvFunctionParameter.name: String get() = this.bindingPat.name
