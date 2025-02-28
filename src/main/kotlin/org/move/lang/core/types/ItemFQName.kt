package org.move.lang.core.types

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.ext.parentModule
import org.move.lang.moveProject

fun MvModule.indexIds(): Set<String> = setOfNotNull(this.indexId())

fun MvModule.indexId(): String? {
    return this.fqName()?.declarationText()
}

fun MvNamedElement.fqName(): ItemFQName? {
    val name = this.name ?: return null
    return when (this) {
        is MvModule -> {
            val moveProject = this.moveProject
            val address = this.address(moveProject) ?: Address.Value("0x0")
            ItemFQName.Module(address, name)
        }
        is MvStruct, is MvEnum -> {
            val moduleFQName = this.module.fqName() as? ItemFQName.Module ?: return null
            ItemFQName.Item(moduleFQName, name)
        }
        is MvFunctionLike -> {
            val moduleFQName = this.module?.fqName() as? ItemFQName.Module ?: return null
            ItemFQName.Item(moduleFQName, name)
        }
        is MvConst -> {
            val moduleFQName = this.module?.fqName() as? ItemFQName.Module ?: return null
            ItemFQName.Item(moduleFQName, name)
        }
        is MvSchema -> {
            val moduleFQName = this.parentModule?.fqName() as? ItemFQName.Module ?: return null
            ItemFQName.Item(moduleFQName, name)
        }
        else -> null
    }
}

sealed class ItemFQName {
    data class Module(val address: Address, val name: String): ItemFQName()
    data class Item(val moduleName: Module, val name: String): ItemFQName()

    fun name(): String = when (this) {
        is Module -> this.name
        is Item -> this.name
    }

    fun containerName(): String = when (this) {
        is Module -> this.address.declarationText()
        is Item -> this.moduleName.declarationText()
    }

    fun moduleDeclarationText(): String? {
        return when (this) {
            is Module -> this.declarationText()
            is Item -> this.moduleName.declarationText()
        }
    }

    fun declarationText(): String {
        return when (this) {
            is Module -> {
                val addressText = this.address.declarationText()
                return "$addressText::${this.name}"
            }
            is Item -> {
                val moduleDeclarationText = this.moduleName.declarationText()
                return "$moduleDeclarationText::${this.name}"
            }
        }
    }

    fun cmdText(): String {
        return when (this) {
            is Module -> {
                val addressText = this.address.shortenedValueText()
                "$addressText::${this.name}"
            }
            is Item -> {
                val moduleCmdText = this.moduleName.cmdText()
                "$moduleCmdText::${this.name}"
            }
        }
    }
}
