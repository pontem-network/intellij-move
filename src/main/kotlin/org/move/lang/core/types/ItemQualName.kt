package org.move.lang.core.types

import org.move.cli.MoveProject
import org.move.lang.core.psi.MvQualNamedElement
import org.move.lang.moveProject

data class ItemQualName(
    val item: MvQualNamedElement,
    val address: Address,
    val moduleName: String?,
    val itemName: String
) {
    fun editorText(): String {
        val addressText = when (address) {
            is Address.Named -> address.name
            is Address.Value -> address.addressLit().original
        }
        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
    }

    fun cmdText(moveProject: MoveProject? = null): String {
        val addressText = when (address) {
            is Address.Value -> address.addressLit().canonical()
            is Address.Named -> {
                val addressMoveProject = moveProject ?: item.moveProject ?: error("No move project found")
                address.addressLit(addressMoveProject)?.canonical()
            }
        }
        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
    }

    fun cmdTextFromProject(moveProject: MoveProject): String {
        val addressText = when (address) {
            is Address.Value -> address.addressLit().canonical()
            is Address.Named -> {
                val moveProject = item.moveProject ?: error("No MoveProject found")
                address.addressLit(moveProject)?.canonical()
            }
        }
        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
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
