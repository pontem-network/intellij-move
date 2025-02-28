package org.move.lang.core.types

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.ext.parentModule
import org.move.lang.moveProject

fun MvNamedElement.fqName(): ItemFQName? {
    val itemName = this.name ?: return null
    return when (this) {
        is MvModule -> {
            val moveProject = this.moveProject
            // from stub
            val address = this.address(moveProject) ?: Address.Value("0x0")
            ItemFQName(address, null, itemName)
        }
        is MvStruct, is MvEnum -> {
            val moduleFQName = this.module.fqName() ?: return null
            ItemFQName(moduleFQName.address, moduleFQName.itemName, itemName)
        }
        is MvFunctionLike -> {
            val moduleFQName = this.module?.fqName() ?: return null
            ItemFQName(moduleFQName.address, moduleFQName.itemName, itemName)
        }
        is MvConst -> {
            val moduleFQName = this.module?.fqName() ?: return null
            ItemFQName(moduleFQName.address, moduleFQName.itemName, itemName)
        }
        is MvSchema -> {
            val moduleFQName = this.parentModule?.fqName() ?: return null
            ItemFQName(moduleFQName.address, moduleFQName.itemName, itemName)
        }
        else -> null
    }
}

data class ItemFQName(
    val address: Address,
    val moduleName: String?,
    val itemName: String
) {
    fun editorModuleFqName(): String? {
        val addressText = this.address.editorText()
        return moduleName?.let { "$addressText::$it" }
    }

    fun editorText(): String {
        val addressText = this.address.editorText()
        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
    }

    fun cmdText(): String {
        val addressText = this.address.shortenedValueText()
        return listOfNotNull(addressText, moduleName, itemName).joinToString("::")
    }
}
