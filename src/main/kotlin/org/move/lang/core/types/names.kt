package org.move.lang.core.types

fun String.shortenYamlAddress(): String {
    if (!this.startsWith("0")) return this
    val trimmed = if (!this.startsWith("0x")) {
        this.substring(1 until this.length)
    } else {
        this.substring(2 until this.length)
    }.trimStart('0')
    return "0x$trimmed"
}

//data class Address(val text: String) {
//    fun normalized(): Address {
//        if (!text.startsWith("0")) return this
//        val trimmed = if (!text.startsWith("0x")) {
//            text.substring(1 until text.length)
//        } else {
//            text.substring(2 until text.length)
//        }
//        return Address("0x" + trimmed.padStart(MAX_LENGTH, '0'))
//    }
//
//    fun shortText(): String {
//        val n = this.normalized().text
//        if (n == "_") return n
//        val trimmed = n.substring(2 until n.length).trimStart('0')
//        return "0x$trimmed"
//    }
//
//    companion object {
//        const val MAX_LENGTH = 32;
//
//        fun default(): Address = Address("0x1")
//    }
//}

data class FQModule(val address: Address, val name: String)
