package org.move.lang.core.types

import org.move.cli.MoveProject
import org.move.lang.core.psi.MvAddressRef
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.addressRef

data class NumericAddress(val value: String) {
    fun short(): String = trimmedValue(value)
    fun normalized(): String = short()

    override fun hashCode(): Int {
        return this.value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NumericAddress) {
            return false
        }
        return this.short() == other.short()
    }

    companion object {
        fun trimmedValue(text: String): String {
            if (!text.startsWith("0")) return text
            val trimmed =
                if (!text.startsWith("0x")) {
                    text.substring(1 until text.length)
                } else {
                    text.substring(2 until text.length)
                }
            var trimmedAddress = trimmed.trimStart('0')
            if (trimmedAddress.isBlank()) {
                // 0x0
                trimmedAddress = "0"
            }
            return "0x$trimmedAddress"
        }
    }
}

sealed class Address {
    val is0x0: Boolean
        get() = this is Value && this.numericAddress.short() == "0x0"

    fun is0x1(moveProject: MoveProject): Boolean {
        val numericAddress = this.resolveToNumericAddress(moveProject) ?: return false
        return numericAddress == NumericAddress("0x1")
    }

    data class Value(val numericAddress: NumericAddress): Address() {
        constructor(value: String):
                this(NumericAddress(value))
    }

    data class Named(val name: String): Address()

    fun identifierText(): String {
        return when (this) {
            is Named -> this.name
            is Value -> this.numericAddress.value
        }
    }

    fun normalizedText(): String {
        // returns Address.Named for named address, and normalized Address.Value for value address
        return when (this) {
            is Named -> this.name
            is Value -> this.numericAddress.normalized()
        }
    }

    fun resolveToNumericAddress(moveProject: MoveProject?): NumericAddress? {
        return when (this) {
            is Named -> moveProject?.getNumericAddressByName(name)
            is Value -> this.numericAddress
        }
    }

    fun indexId(): String = this.normalizedText()

    fun searchIndexIds(moveProject: MoveProject): Set<String> {
        val address = this
        return buildSet {
            if (address is Named) {
                add(address.name)
            }
            val numericAddress = address.resolveToNumericAddress(moveProject)
            if (numericAddress != null) {
                add(numericAddress.normalized())
                addAll(moveProject.getAddressNamesForValue(numericAddress))
            }
        }
    }

    companion object {
        fun equals(left: Address, right: Address, moveProject: MoveProject): Boolean {
            if (left === right) return true
            val leftNumeric = left.resolveToNumericAddress(moveProject) ?: return false
            val rightNumeric = right.resolveToNumericAddress(moveProject) ?: return false
            return leftNumeric.normalized() == rightNumeric.normalized()
        }
    }
}

fun MvModule.address(): Address? = this.addressRef()?.refToAddress()

fun MvAddressRef.refToAddress(): Address? {
    val namedAddress = this.namedAddress
    if (namedAddress != null) {
        return Address.Named(namedAddress.referenceName)
    }
    val value = this.diemAddress?.text ?: this.integerLiteral?.text ?: return null
    return Address.Value(value)
}
