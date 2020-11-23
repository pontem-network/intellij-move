package org.move.lang.core.types

data class Address(val text: String) {
    companion object {
        fun default(): Address = Address("0x1")
    }
}
