package org.move.lang.core.types

import org.move.cli.MoveProject
import org.move.lang.core.psi.MvAddressRef
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.addressRef
import org.move.lang.core.types.AddressValue.Companion.normalizeValue

const val MAX_LENGTH = 32

class AddressValue(val value: String) {
    fun canonical(): String = normalizeValue(value)
    fun short(): String = shortenValue(value)

    companion object {
        fun normalizeValue(text: String): String {
            if (!text.startsWith("0")) return text
            val trimmed = if (!text.startsWith("0x")) {
                text.substring(1 until text.length)
            } else {
                text.substring(2 until text.length)
            }
            return "0x" + trimmed.padStart(MAX_LENGTH, '0')
        }

        fun shortenValue(text: String): String {
            if (!text.startsWith("0")) return text
            val trimmed = if (!text.startsWith("0x")) {
                text.substring(1 until text.length)
            } else {
                text.substring(2 until text.length)
            }
            return "0x" + trimmed.trimStart('0')
        }
    }
}

sealed class Address {

    abstract fun text(): String
    abstract fun canonicalValue(): String?

    val is0x0 get() = this is Value && this.addressValue().value == "0x0"
    val is0x1 get() = canonicalValue() == "0x00000000000000000000000000000001"

    class Value(private val value: String): Address() {
        fun addressValue(): AddressValue = AddressValue(value)

        override fun canonicalValue(): String = this.addressValue().canonical()

        override fun text(): String = this.addressValue().value

        override fun toString(): String = "Address.Value($value)"
    }

    class Named(val name: String, val value: String?): Address() {
        fun addressValue(): AddressValue? = this.value?.let { AddressValue(it) }

        override fun canonicalValue(): String? = this.addressValue()?.canonical()

        override fun text(): String = "$name = $value"
    }

    fun declarationText(): String {
        return when (this) {
            is Named -> this.name
            is Value -> this.addressValue().value
        }
    }

    fun universalText(): String {
        // returns Address.Named for named address, and normalized Address.Value for value address
        return when (this) {
            is Named -> this.name
            is Value -> this.addressValue().canonical()
        }
    }

    fun shortenedValueText(): String? {
        return when (this) {
            is Named -> this.addressValue()?.short()
            is Value -> this.addressValue().short()
        }
    }

    fun canonicalValueText(): String? {
        return when (this) {
            is Named -> this.addressValue()?.canonical()
            is Value -> this.addressValue().canonical()
        }
    }

    companion object {
        fun equals(left: Address?, right: Address?): Boolean {
            if (left === right) return true
            if (left == null && right == null) return true
            return when {
                left is Value && right is Value ->
                    left.addressValue().canonical() == right.addressValue().canonical()
                left is Named && right is Named -> {
                    val leftValue = left.value?.let { normalizeValue(it) }
                    val rightValue = right.value?.let { normalizeValue(it) }
                    if (leftValue == null && rightValue == null) {
                        // null items cannot be equal
                        return false
                    }
                    return leftValue == rightValue
                }
                left is Value && right is Named -> checkValueNamedEquals(left, right)
                left is Named && right is Value -> checkValueNamedEquals(right, left)
                else -> false
            }
        }

        private fun checkValueNamedEquals(value: Value, named: Named): Boolean {
            val normalizedValue = value.addressValue().canonical()
            val normalizedNamed = named.value?.let { normalizeValue(it) }
            return normalizedValue == normalizedNamed
        }
    }
}

fun MvModule.address(moveProject: MoveProject?): Address? = this.addressRef()?.address(moveProject)

fun MvAddressRef.address(moveProject: MoveProject?): Address? {
    val namedAddress = this.namedAddress
    if (namedAddress != null) {
        val name = namedAddress.referenceName
        return if (moveProject == null) {
            Address.Named(name, null)
        } else {
            moveProject.getNamedAddressTestAware(name) ?: Address.Named(name, null)
        }
    }

    val addressText = this.diemAddress?.text ?: this.integerLiteral?.text ?: return null
    return Address.Value(addressText)
}
