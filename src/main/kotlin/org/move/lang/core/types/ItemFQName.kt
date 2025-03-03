package org.move.lang.core.types

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.ext.parentModuleOrModuleSpec
import org.move.lang.core.resolve.PathKind
import org.move.lang.core.resolve.pathKind
import org.move.lang.moveProject

fun moduleIndexId(address: Address, name: String): String {
    return "${address.universalText()}::$name"
}

fun addressIndexIds(address: Address): List<String> {
    return listOfNotNull(
        address.universalText(), address.canonicalValueText()
    )
}

fun moduleIndexIds(address: Address, name: String): HashSet<String> {
    return addressIndexIds(address).map { "$it::$name" }.toHashSet()
}

fun MvModule.indexIds(): HashSet<String> {
    val fqName = this.fqName() as? ItemFQName.Module ?: return hashSetOf()
    return moduleIndexIds(fqName.address, fqName.name)
}

fun MvElement.fqName(): ItemFQName? {
    return when (this) {
        is MvModule -> {
            val name = this.name ?: return null
            val moveProject = this.moveProject
            val address = this.address(moveProject) ?: Address.Value("0x0")
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
            val name = path.referenceName ?: return null
            val pathKind = path.pathKind(isCompletion = false)
            return when (pathKind) {
                is PathKind.QualifiedPath.Module -> ItemFQName.Module(pathKind.address, name)
                is PathKind.QualifiedPath.ModuleOrItem -> ItemFQName.Module(pathKind.address, name)
                else -> null
            }
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

    fun containerName(): String = when (this) {
        is Module -> this.address.declarationText()
        is Item -> this.moduleFQName.declarationText()
    }

    fun moduleDeclarationText(): String? {
        return when (this) {
            is Module -> this.declarationText()
            is Item -> this.moduleFQName.declarationText()
        }
    }

    fun declarationText(): String {
        return when (this) {
            is Module -> {
                val addressText = this.address.declarationText()
                return "$addressText::${this.name}"
            }
            is Item -> {
                val moduleDeclarationText = this.moduleFQName.declarationText()
                return "$moduleDeclarationText::${this.name}"
            }
        }
    }

    fun universalAddressText(): String {
        return when (this) {
            is Module -> {
                val addressText = this.address.universalText()
                "$addressText::${this.name}"
            }
            is Item -> {
                val moduleCmdText = this.moduleFQName.universalAddressText()
                "$moduleCmdText::${this.name}"
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

    fun canonicalAddressValueText(): String {
        return when (this) {
            is Module -> {
                val addressText = this.address.canonicalValueText()
                "$addressText::${this.name}"
            }
            is Item -> {
                val moduleCmdText = this.moduleFQName.canonicalAddressValueText()
                "$moduleCmdText::${this.name}"
            }
        }
    }

    fun shortAddressValueText(): String {
        return when (this) {
            is Module -> {
                val addressText = this.address.shortenedValueText()
                "$addressText::${this.name}"
            }
            is Item -> {
                val moduleCmdText = this.moduleFQName.shortAddressValueText()
                "$moduleCmdText::${this.name}"
            }
        }
    }
}
