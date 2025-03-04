package org.move.lang.core.types

import org.move.cli.MoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.ext.parentModuleOrModuleSpec
import org.move.lang.core.psi.ext.qualifier

fun MvElement.fqName(): ItemFQName? {
    return when (this) {
        is MvModule -> {
            val name = this.name ?: return null
            val address = this.address() ?: return null
            ItemFQName.Module(address, name)
        }
        is MvStruct, is MvEnum -> {
            val name = this.name ?: return null
            val moduleFQName = this.module.fqName() as? ItemFQName.Module ?: return null
            ItemFQName.Item(moduleFQName, name)
        }
        is MvFunction -> {
            val name = this.name ?: return null
            val moduleFQName = this.module?.fqName() as? ItemFQName.Module ?: return null
            ItemFQName.Item(moduleFQName, name)
        }
        is MvSpecFunction, is MvSpecInlineFunction, is MvSchema -> {
            val name = this.name ?: return null
            val moduleFQName = this.parentModuleOrModuleSpec?.fqName() as? ItemFQName.Module ?: return null
            ItemFQName.Item(moduleFQName, name)
        }
        is MvModuleSpec -> {
            val path = this.path ?: return null
            val qualifier = path.qualifier ?: return null
            val name = path.referenceName ?: return null

            // three element path cannot be present in module spec
            if (qualifier.qualifier != null) return null

            val qualifierAddress = qualifier.pathAddress
            if (qualifierAddress != null) {
                val address = Address.Value(qualifierAddress.text)
                return ItemFQName.Module(address, name)
            }

            val qualifierName = qualifier.referenceName ?: return null
            return ItemFQName.Module(Address.Named(qualifierName), name)
        }
        is MvConst -> {
            val name = this.name ?: return null
            val moduleFQName = this.module?.fqName() as? ItemFQName.Module ?: return null
            ItemFQName.Item(moduleFQName, name)
        }
        else -> null
    }
}

sealed class ItemFQName {
    data class Module(val address: Address, val name: String): ItemFQName()
    data class Item(val moduleFQName: ItemFQName, val name: String): ItemFQName()

    fun name(): String = when (this) {
        is Module -> this.name
        is Item -> this.name
    }

    fun qualifierName(): String =
        when (this) {
            is Module -> this.address.identifierText()
            is Item -> this.moduleFQName.identifierText()
        }

    fun moduleText(): String? {
        return when (this) {
            is Module -> this.identifierText()
            is Item -> this.moduleFQName.identifierText()
        }
    }

    fun identifierText(): String {
        return when (this) {
            is Module -> {
                val addressText = this.address.identifierText()
                return "$addressText::${this.name}"
            }
            is Item -> {
                val moduleDeclarationText = this.moduleFQName.identifierText()
                return "$moduleDeclarationText::${this.name}"
            }
        }
    }

    fun indexId(): String {
        return when (this) {
            is Module -> {
                val addressText = this.address.indexId()
                "$addressText::${this.name}"
            }
            is Item -> {
                val moduleCmdText = this.moduleFQName.indexId()
                "$moduleCmdText::${this.name}"
            }
        }
    }

    fun commandLineText(moveProject: MoveProject?): String? {
        return when (this) {
            is Module -> {
                val addressText = this.address.resolveToNumericAddress(moveProject)?.short() ?: return null
                "$addressText::${this.name}"
            }
            is Item -> {
                val moduleCmdText = this.moduleFQName.commandLineText(moveProject) ?: return null
                "$moduleCmdText::${this.name}"
            }
        }
    }
}
