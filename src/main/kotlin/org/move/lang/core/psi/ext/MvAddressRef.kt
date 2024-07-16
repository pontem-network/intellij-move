package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvAddressRef
import org.move.lang.moveProject

val MvAddressRef.normalizedText: String get() = this.text.lowercase()
