package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvAddressRef
import org.move.lang.moveProject

val MvAddressRef.normalizedText: String get() = this.text.lowercase()

val MvAddressRef.useGroupLevel: Int
    get() {
        // sort to the end if not a named address
        if (this.namedAddress == null) return 4

        val name = this.namedAddress?.text.orEmpty().lowercase()
        val currentPackageAddresses =
            this.moveProject?.currentPackageAddresses()?.keys.orEmpty().map { it.lowercase() }
        return when (name) {
            "std" -> 0
            "aptos_std", "aptos_framework" -> 1
            !in currentPackageAddresses -> 2
            else -> 3
        }
    }
