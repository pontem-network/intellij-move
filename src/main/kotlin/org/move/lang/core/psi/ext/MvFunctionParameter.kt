package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvFunctionParameter

// BindingPat has required name
val MvFunctionParameter.name: String get() = this.bindingPat.name
