package org.move.lang.core.types

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.move.cli.MoveProject
import org.move.lang.core.psi.MvAddressRef
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.addressRef
import org.move.lang.core.psi.ext.greenStub
import org.move.lang.core.types.Address.Named
import org.move.lang.core.types.Address.Value
import org.move.lang.core.types.AddressLit.Companion.normalizeValue

const val MAX_LENGTH = 32

class AddressLit(val original: String) {
    fun canonical(): String = normalizeValue(original)
    fun short(): String = shortenValue(original)

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
    abstract fun canonicalValue(moveProject: MoveProject): String?

    val is0x0 get() = this is Value && this.addressLit().original == "0x0"

    class Value(private val value: String): Address() {
        fun addressLit(): AddressLit = AddressLit(value)

        override fun canonicalValue(moveProject: MoveProject): String = this.addressLit().canonical()

        override fun text(): String = this.addressLit().original

//        override fun equals(other: Any?): Boolean {
//            if (this === other) return true
//            if (other !is Value) return false
//            if (this.hashCode() != other.hashCode()) return false
//            return eq(this, other)
//        }

//        override fun hashCode(): Int = normalizeValue(value).hashCode()

        override fun toString(): String = "Address.Value($value)"
    }

    class Named(
        val name: String,
        val value: String?,
        private val declMoveProject: MoveProject?
    ): Address() {
        fun value(moveProject: MoveProject? = null): String {
            return value
                ?: this.declMoveProject?.getNamedAddressValue(name)
                ?: moveProject?.getNamedAddressValue(name)
                ?: UNKNOWN
        }

        fun addressLit(moveProject: MoveProject): AddressLit? =
            moveProject.getNamedAddressValue(this.name)?.let { AddressLit(it) }

        override fun canonicalValue(moveProject: MoveProject): String? =
            this.addressLit(moveProject)?.canonical()

        override fun text(): String = "$name = ${value()}"

//        override fun equals(other: Any?): Boolean {
//            if (this === other) return true
//            if (other !is Named) return false
////            if (this.hashCode() != other.hashCode()) return false
//            return eq(this, other)
//        }

//        override fun hashCode(): Int = name.hashCode()
    }

    companion object {
        const val UNKNOWN: String = "0x0"

        fun equals(left: Address?, right: Address?): Boolean {
            if (left === right) return true
            if (left == null && right == null) return true
            return when {
                left is Value && right is Value ->
                    left.addressLit().canonical() == right.addressLit().canonical()
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
            val normalizedValue = value.addressLit().canonical()
            val normalizedNamed = named.value?.let { normalizeValue(it) }
            return normalizedValue == normalizedNamed
        }
    }
}

sealed class StubAddress {
    object Unknown: StubAddress()
    data class Value(val value: String): StubAddress()
    data class Named(val name: String): StubAddress()

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
                    Named(this.name, null, null)
                } else {
                    moveProject.getNamedAddressTestAware(this.name) ?: Named(
                        this.name,
                        null,
                        moveProject
                    )
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

fun StubOutputStream.serializeStubAddress(stubAddress: StubAddress) {
    writeInt(stubAddress.asInt())
    when (stubAddress) {
        is StubAddress.Value -> writeUTFFast(stubAddress.value)
        is StubAddress.Named -> writeUTFFast(stubAddress.name)
        is StubAddress.Unknown -> {}
    }
}

fun StubInputStream.deserializeStubAddress(): StubAddress {
    val addressInt = this.readInt()
    return when (addressInt) {
        StubAddress.UNKNOWN_INT -> StubAddress.Unknown
        StubAddress.VALUE_INT -> StubAddress.Value(this.readUTFFast())
        StubAddress.NAMED_INT -> StubAddress.Named(this.readUTFFast())
        else -> error("Invalid value")
    }
}


val MvModule.stubAddress: StubAddress
    get() {
        val stub = greenStub
        return stub?.address ?: this.psiStubAddress()
    }

fun MvModule.addressAsCanonicalValue(moveProject: MoveProject): String? =
    this.address(moveProject)?.canonicalValue(moveProject)

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
