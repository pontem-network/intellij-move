package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvAddressRef

val MvAddressRef.normalizedText: String get() = this.text.lowercase()
