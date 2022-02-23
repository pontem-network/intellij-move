package org.move.lang.core.types

const val MAX_LENGTH = 32;

fun normalizeAddressValue(text: String): String {
    if (!text.startsWith("0")) return text
    val trimmed = if (!text.startsWith("0x")) {
        text.substring(1 until text.length)
    } else {
        text.substring(2 until text.length)
    }
    return "0x" + trimmed.padStart(MAX_LENGTH, '0')
}

sealed class Address(open val value: String) {
    abstract fun text(): String
//    fun text(): String {
//
//    }
//
//    fun shortValue(): String {
//        val n = this.normalized().text
//        if (n == "_") return n
//        val trimmed = n.substring(2 until n.length).trimStart('0')
//        return "0x$trimmed"
//    }

    data class Value(override val value: String) : Address(value) {
        override fun text(): String {
            return value
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Value) return false
            if (this.hashCode() != other.hashCode()) return false
            return normalizeAddressValue(value) == normalizeAddressValue(other.value)
        }

        override fun hashCode(): Int {
            return normalizeAddressValue(value).hashCode()
        }
    }

    data class Named(val name: String, override val value: String) : Address(value) {
        override fun text(): String {
            return "$name = $value"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Named) return false
            if (this.hashCode() != other.hashCode()) return false
            return Pair(name, normalizeAddressValue(value)) == Pair(name, normalizeAddressValue(other.value))
        }

        override fun hashCode(): Int {
            return (name + value).hashCode()
        }
    }
}
