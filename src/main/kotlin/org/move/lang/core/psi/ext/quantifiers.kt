package org.move.lang.core.psi.ext

import com.intellij.psi.util.descendantsOfType
import org.move.lang.core.psi.MvBindingPat
import org.move.lang.core.psi.MvExistsQuantifier
import org.move.lang.core.psi.MvForallQuantifier

val MvForallQuantifier.bindings
    get() = this.quantifierBindings?.descendantsOfType<MvBindingPat>().orEmpty().toList()

val MvExistsQuantifier.bindings
    get() = this.quantifierBindings?.descendantsOfType<MvBindingPat>().orEmpty().toList()
