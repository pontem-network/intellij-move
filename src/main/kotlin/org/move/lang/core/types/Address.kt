package org.move.lang.core.types

import org.move.cli.MoveProject
import org.move.lang.core.psi.MvAddressRef
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.addressRef
import org.move.lang.core.psi.ext.greenStub

const val MAX_LENGTH = 32

sealed class Address(open val value: String) {

    val canonicalValue: String get() = normalizeValue(this.value)
    val shortenedValue: String get() = shortenValue(this.value, 8)

    abstract fun text(): String

    class Value(override val value: String) : Address(value) {
        override fun text(): String = value

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Value) return false
            if (this.hashCode() != other.hashCode()) return false
            return normalizeValue(value) == normalizeValue(other.value)
        }

        override fun hashCode(): Int {
            return normalizeValue(value).hashCode()
        }
    }

    class Named(val name: String, override val value: String) : Address(value) {
        override fun text(): String {
            return "$name = $value"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Named) return false
            if (this.hashCode() != other.hashCode()) return false
            return Pair(name, normalizeValue(value)) == Pair(name, normalizeValue(other.value))
        }

        override fun hashCode(): Int {
            return (name + value).hashCode()
        }
    }

    companion object {
        private fun normalizeValue(text: String): String {
            if (!text.startsWith("0")) return text
            val trimmed = if (!text.startsWith("0x")) {
                text.substring(1 until text.length)
            } else {
                text.substring(2 until text.length)
            }
            return "0x" + trimmed.padStart(MAX_LENGTH, '0')
        }

        private fun shortenValue(text: String, maxLength: Int): String {
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

sealed class StubAddress {
    object Unknown : StubAddress()
    data class Value(val value: String) : StubAddress()
    data class Named(val name: String) : StubAddress()

    fun asInt(): Int {
        return when (this) {
            is Unknown -> UNKNOWN_INT
            is Value -> VALUE_INT
            is Named -> NAMED_INT
        }
    }

    fun asAddress(moveProject: MoveProject?): Address? {
        return when (this) {
            is Named -> moveProject?.getNamedAddress(this.name)
            is Value -> Address.Value(this.value)
            is Unknown -> null
        }
    }

    companion object {
        const val UNKNOWN_INT = 0
        const val VALUE_INT = 1
        const val NAMED_INT = 2
    }
}

val MvModule.stubAddress: StubAddress
    get() {
        val stub = greenStub
        return stub?.address ?: this.psiStubAddress()
    }

fun MvModule.address(proj: MoveProject?): Address? = this.stubAddress.asAddress(proj)

fun MvAddressRef.address(proj: MoveProject?): Address? = psiStubAddress().asAddress(proj)

fun MvModule.psiStubAddress(): StubAddress =
    this.addressRef()?.psiStubAddress() ?: StubAddress.Unknown

fun MvAddressRef.psiStubAddress(): StubAddress {
    val namedAddress = this.namedAddress
    if (namedAddress != null) {
        return StubAddress.Named(namedAddress.referenceName)
    }
    val addressText = this.diemAddress?.text ?: this.bech32Address?.text ?: return StubAddress.Unknown
    return StubAddress.Value(addressText)
}
