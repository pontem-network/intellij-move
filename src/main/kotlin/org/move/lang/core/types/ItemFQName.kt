package org.move.lang.core.types

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.move.cli.runConfigurations.aptos.run.Transaction
import org.move.openapiext.readUTFFastAsNullable
import org.move.openapiext.writeUTFFastAsNullable

data class ItemFQName(
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

    fun cmdText(): String {
        val addressText = address.canonicalValue
        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
    }

    fun shortCmdText(): String {
        val addressText = address.shortenedValue
        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
    }

//    fun cmdTextWithShortenedAddress(): String {
//        val addressText = address.value
//        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
//    }

    companion object {
        val DEFAULT_MOD_FQ_NAME: ItemFQName =
            ItemFQName(Address.Value("0x0"), null, "default")
        val DEFAULT_ITEM_FQ_NAME: ItemFQName =
            ItemFQName(Address.Value("0x0"), "default", "default_item")

        fun fromCmdText(text: String): ItemFQName? {
            val parts = text.split("::")
            val address = parts.getOrNull(0) ?: return null
            val moduleName = parts.getOrNull(1) ?: return null
            val itemName = parts.getOrNull(2) ?: return null
            return ItemFQName(Address.Value(address), moduleName, itemName)
        }

        fun serialize(fqName: ItemFQName, dataStream: StubOutputStream) {
            with(dataStream) {
                writeUTFFast(fqName.address.text())
                writeUTFFastAsNullable(fqName.moduleName)
                writeUTFFast(fqName.itemName)
            }
        }

        fun deserialize(dataStream: StubInputStream): ItemFQName {
            val addressText = dataStream.readUTFFast()
            val address =
                if ("=" in addressText) {
                    val parts = addressText.split("=")
                    val name = parts[0].trim()
                    val value = parts[1].trim()
                    Address.Named(name, value)
                } else {
                    Address.Value(addressText)
                }
            val moduleName = dataStream.readUTFFastAsNullable()
            val itemName = dataStream.readUTFFast()
            return ItemFQName(address, moduleName, itemName)
        }
    }
}
