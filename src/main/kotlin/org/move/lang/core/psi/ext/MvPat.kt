package org.move.lang.core.psi.ext

import com.intellij.psi.util.descendantsOfType
import org.move.lang.core.psi.MvPat
import org.move.lang.core.psi.MvPatBinding

val MvPat.bindings: List<MvPatBinding> get() = this.descendantsOfType<MvPatBinding>().toList()
