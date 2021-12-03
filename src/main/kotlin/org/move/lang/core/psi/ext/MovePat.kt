package org.move.lang.core.psi.ext

import com.intellij.psi.util.descendantsOfType
import org.move.lang.core.psi.MoveBindingPat
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MovePat

val MovePat.bindings: List<MoveNamedElement> get() = this.descendantsOfType<MoveBindingPat>().toList()
