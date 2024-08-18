package org.move.lang.core.psi.ext

import com.intellij.psi.util.descendantsOfType
import org.move.lang.core.psi.MvPatBinding
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvPat
import org.move.lang.core.psi.MvPatIdent

val MvPat.bindings: List<MvNamedElement> get() = this.descendantsOfType<MvPatBinding>().toList()

val MvPatIdent.patBinding: MvPatBinding get() = childOfType<MvPatBinding>()!!
