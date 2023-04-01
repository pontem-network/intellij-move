package org.move.lang.core.types

import org.move.cli.MoveProject

data class ItemQualName(
    val address: Address,
    val moduleName: String?,
    val itemName: String
) {
    fun editorText(): String {
        val addressText = when (address) {
            is Address.Named -> address.name
            is Address.Value -> address.value
        }
        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
    }

    fun cmdText(moveProject: MoveProject? = null): String {
        val addressText = address.canonicalValue(moveProject)
        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
    }

    fun shortCmdText(): String {
        val addressText = address.shortenedValue(null)
        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
    }

//    fun cmdTextWithShortenedAddress(): String {
//        val addressText = address.value
//        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
//    }

    companion object {
        val DEFAULT_MOD_FQ_NAME: ItemQualName =
            ItemQualName(Address.Value("0x0"), null, "default")
        val ANY_SCRIPT: ItemQualName =
            ItemQualName(Address.Value("0x0"), null, "script")

        fun fromCmdText(text: String): ItemQualName? {
            val parts = text.split("::")
            val address = parts.getOrNull(0) ?: return null
            val moduleName = parts.getOrNull(1) ?: return null
            val itemName = parts.getOrNull(2) ?: return null
            return ItemQualName(Address.Value(address), moduleName, itemName)
        }
    }
}
