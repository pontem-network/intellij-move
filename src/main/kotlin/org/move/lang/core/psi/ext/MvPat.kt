package org.move.lang.core.psi.ext

import com.intellij.psi.util.descendantsOfType
import org.move.lang.core.psi.MvBindingPat
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvPat

val MvPat.bindings: Sequence<MvNamedElement> get() = this.descendantsOfType<MvBindingPat>()
