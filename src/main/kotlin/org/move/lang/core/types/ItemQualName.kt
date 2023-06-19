package org.move.lang.core.types

import org.move.cli.MoveProject
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

    fun cmdText(moveProject: MoveProject): String {
        val addressText = when (address) {
            is Address.Named -> address.addressLit(moveProject)?.short()
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

    companion object {
        fun qualNameForCompletion(qualName: String): String? {
            val parts = qualName.split("::")
            val address = parts.getOrNull(0) ?: return null
            val moduleName = parts.getOrNull(1) ?: return null
            val itemName = parts.getOrNull(2) ?: return null
            return "${AddressLit(address).short()}::$moduleName::$itemName"
        }

        fun split(qualName: String): Triple<String, String?, String>? {
            val parts = qualName.split("::")
            val address = parts.getOrNull(0) ?: return null
            val moduleName = parts.getOrNull(1) ?: return null
            val itemName = parts.getOrNull(2)
            if (itemName == null) {
                return Triple(address, null, moduleName)
            } else {
                return Triple(address, moduleName, itemName)
            }
        }
    }
}
