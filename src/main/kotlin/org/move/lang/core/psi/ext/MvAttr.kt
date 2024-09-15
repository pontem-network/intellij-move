package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvAttr

val MvAttr.attributeOwner: MvDocAndAttributeOwner? get() = this.parent as? MvDocAndAttributeOwner
