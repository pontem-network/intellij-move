package org.move.lang.core.types

import org.move.lang.core.psi.MvQualNamedElement

data class ItemQualName(
    val item: MvQualNamedElement,
    val address: Address,
    val moduleName: String?,
    val itemName: String
) {
    fun editorModuleFqName(): String? {
        return moduleName?.let { "${this.editorAddressText()}::$it" }
    }

    fun editorText(): String {
        val addressText = this.editorAddressText()
        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
    }

    fun cmdText(): String {
        val addressText = when (address) {
            is Address.Named -> address.addressLit()?.short()
            is Address.Value -> address.addressLit().short()
        }
        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
    }

    private fun editorAddressText(): String {
        return when (address) {
            is Address.Named -> address.name
            is Address.Value -> address.addressLit().original
        }
    }
}
