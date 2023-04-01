package org.move.lang.core.types

import org.move.cli.MoveProject
import org.move.lang.core.psi.MvAddressRef
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.addressRef
import org.move.lang.core.psi.ext.greenStub

const val MAX_LENGTH = 32

sealed class Address {

    abstract fun canonicalValue(moveProject: MoveProject?): String
    abstract fun text(): String

    fun shortenedValue(moveProject: MoveProject?): String = shortenValue(canonicalValue(moveProject))

    class Value(val value: String) : Address() {
        override fun canonicalValue(moveProject: MoveProject?): String {
            return normalizeValue(this.value)
        }

        override fun text(): String = value

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Value) return false
            if (this.hashCode() != other.hashCode()) return false
            return eq(this, other)
        }

        override fun hashCode(): Int = normalizeValue(value).hashCode()
    }

    class Named(val name: String, private val _value: String?, private val declMoveProject: MoveProject?) : Address() {
        fun value(moveProject: MoveProject? = null): String {
            return _value
                ?: this.declMoveProject?.getNamedAddressValue(name)
                ?: moveProject?.getNamedAddressValue(name)
                ?: UNKNOWN
        }

        override fun canonicalValue(moveProject: MoveProject?): String {
            return normalizeValue(this.value(moveProject))
        }

        override fun text(): String = "$name = ${value()}"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Named) return false
            if (this.hashCode() != other.hashCode()) return false
            return eq(this, other)
        }

        override fun hashCode(): Int = name.hashCode()
    }

    companion object {
        const val UNKNOWN: String = "0x0"

        fun eq(left: Address?, right: Address?): Boolean {
            if (left === right) return true
            if (left == null && right == null) return true
            return when {
                left is Value && right is Value -> normalizeValue(left.value) == normalizeValue(right.value)
                left is Named && right is Named ->
                    Pair(left.name, normalizeValue(left.value())) == Pair(
                        right.name,
                        normalizeValue(right.value())
                    )
                else -> false
            }
        }

        private fun normalizeValue(text: String): String {
            if (!text.startsWith("0")) return text
            val trimmed = if (!text.startsWith("0x")) {
                text.substring(1 until text.length)
            } else {
                text.substring(2 until text.length)
            }
            return "0x" + trimmed.padStart(MAX_LENGTH, '0')
        }

        private fun shortenValue(text: String): String {
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
            is Named -> {
                if (moveProject == null) {
                    Address.Named(this.name, null, null)
                } else {
                    moveProject.getNamedAddress(this.name)
                }
            }
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
