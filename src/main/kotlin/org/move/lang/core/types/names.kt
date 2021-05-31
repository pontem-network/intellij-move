package org.move.lang.core.types

data class Address(val text: String) {
    fun normalized(): Address {
        if (!text.startsWith("0x")) return this

        val trimmed = text.substring(2 until text.length)
        return Address("0x" + trimmed.padStart(MAX_LENGTH, '0'))
    }

    companion object {
        const val MAX_LENGTH = 32;

        fun default(): Address = Address("0x1")
    }
}

data class FullyQualModule(val address: Address, val name: String)
