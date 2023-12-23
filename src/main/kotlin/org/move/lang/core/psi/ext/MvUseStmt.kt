package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.stdext.wrapWithList

val MvUseStmt.addressRef: MvAddressRef?
    get() {
        val moduleUseSpeck = this.moduleUseSpeck
        if (moduleUseSpeck != null) {
            val fqModuleRef = moduleUseSpeck.fqModuleRef
            if (fqModuleRef != null) {
                return fqModuleRef.addressRef
            } else {
                return moduleUseSpeck.addressRef
            }
        }
        val itemUseSpeck = this.itemUseSpeck
        if (itemUseSpeck != null) {
            return itemUseSpeck.fqModuleRef.addressRef
        }
        return null
    }

val MvUseStmt.useGroupLevel: Int
    get() {
        if (this.isTestOnly) return 5
        return this.addressRef?.useGroupLevel ?: -1
    }

val MvUseStmt.fqModuleText: String?
    get() {
        val fqModuleRef = this.fqModuleRef ?: return null
        return fqModuleRef.text
    }

val MvUseStmt.fqModuleRef: MvFQModuleRef?
    get() {
        val moduleUseSpeck = this.moduleUseSpeck
        if (moduleUseSpeck != null) {
            return moduleUseSpeck.fqModuleRef
        }
        val itemUseSpeck = this.itemUseSpeck
        if (itemUseSpeck != null) {
            return itemUseSpeck.fqModuleRef
        }
        return null
    }

val MvUseStmt.childUseItems: List<MvUseItem>
    get() {
        return this.itemUseSpeck?.useItems.orEmpty()
//        if (itemUseSpeck != null) {
//            return itemUseSpeck.useItems
////            val group = itemUseSpeck.useItemGroup
////            if (group != null) {
////                return group.useItemList
////            }
////            return itemUseSpeck.useItem.wrapWithList()
//        }
//        return emptyList()
    }

val MvItemUseSpeck.useItems: List<MvUseItem>
    get() {
        val group = this.useItemGroup
        if (group != null) {
            return group.useItemList
        }
        return this.useItem.wrapWithList()
    }

val MvUseStmt.useSpeck: MvUseSpeck? get() = this.itemUseSpeck ?: this.moduleUseSpeck


// TODO: rename
sealed class UseSpeckType(open val nameOrAlias: String) {
    data class Module(
        override val nameOrAlias: String,
        val moduleUseSpeck: MvModuleUseSpeck,
    ): UseSpeckType(nameOrAlias)

    data class SelfModule(
        override val nameOrAlias: String,
        val useItem: MvUseItem,
    ): UseSpeckType(nameOrAlias)

    data class Item(
        override val nameOrAlias: String,
        val useItem: MvUseItem,
    ): UseSpeckType(nameOrAlias)
}

val MvUseStmt.useSpeckTypes: List<UseSpeckType>
    get() {
        val moduleUseSpeck = this.moduleUseSpeck
        if (moduleUseSpeck != null) {
            val nameOrAlias = moduleUseSpeck.nameElement?.text ?: return emptyList()
            return listOf(UseSpeckType.Module(nameOrAlias, moduleUseSpeck))
        }
        return this.itemUseSpeck?.useItems.orEmpty()
            .mapNotNull {
                if (it.isSelf) {
                    val useAlias = it.useAlias
                    val nameOrAlias =
                        if (useAlias != null) {
                            val aliasName = useAlias.name ?: return@mapNotNull null
                            aliasName
                        } else {
                            it.moduleName
                        }
                    UseSpeckType.SelfModule(nameOrAlias, it)
                } else {
                    val nameOrAlias = it.nameOrAlias ?: return@mapNotNull null
                    UseSpeckType.Item(nameOrAlias, it)
                }
            }
    }

val MvUseStmt.useSpeckText: String
    get() {
        val moduleUseSpeck = this.moduleUseSpeck
        if (moduleUseSpeck != null) {
            return moduleUseSpeck.text
        }
        val itemUseSpeck = this.itemUseSpeck
        if (itemUseSpeck != null) {
            return itemUseSpeck.text
        }
        return ""
    }
