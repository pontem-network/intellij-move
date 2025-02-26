package org.move.lang.core.resolve

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvFieldDecl
import org.move.lang.core.resolve.ref.*

fun List<MvNamedElement>.asEntries(): List<ScopeEntry> {
    return this.mapNotNull { it.asEntry() }
}

fun MvNamedElement.asEntry(): ScopeEntry? {
    return when (this) {
        is MvPatBinding -> SimpleScopeEntry(this.name, this, this.itemNs)
        is MvModule -> {
            val name = this.name ?: return null
            SimpleScopeEntry(name, this, this.itemNs)
        }
        is MvFunctionLike -> {
            val name = this.name ?: return null
            ScopeEntryWithVisibility(
                name,
                this,
                this.itemNs,
                itemScopeAdjustment = NamedItemScope.MAIN
            )
        }
        is MvTypeParameter -> {
            val name = this.name ?: return null
            SimpleScopeEntry(name, this, this.itemNs)
        }
        is MvSchema -> {
            val name = this.name ?: return null
            ScopeEntryWithVisibility(
                name,
                this,
                this.itemNs,
                itemScopeAdjustment = NamedItemScope.MAIN
            )
        }
        is MvConst -> {
            val name = this.name ?: return null
            ScopeEntryWithVisibility(
                name,
                this,
                this.itemNs,
                itemScopeAdjustment = NamedItemScope.MAIN
            )
        }
        is MvGlobalVariableStmt -> {
            val name = this.name ?: return null
            ScopeEntryWithVisibility(
                name,
                this,
                this.itemNs,
                itemScopeAdjustment = NamedItemScope.MAIN
            )
        }
        is MvFieldDecl -> {
            val name = this.name ?: return null
            SimpleScopeEntry(name, this, this.itemNs)
        }
        is MvEnumVariant -> {
            val name = this.name ?: return null
            SimpleScopeEntry(name, this, this.itemNs)
        }
        is MvStruct -> {
            val name = this.name ?: return null
            ScopeEntryWithVisibility(
                name,
                this,
                TYPES,
                itemScopeAdjustment = NamedItemScope.MAIN
            )
        }
        is MvEnum -> {
            val name = this.name ?: return null
            ScopeEntryWithVisibility(
                name,
                this,
                ENUMS,
                itemScopeAdjustment = NamedItemScope.MAIN
            )
        }
        is MvLabelDecl -> {
            val name = this.name ?: return null
            SimpleScopeEntry(name, this, NONE)
        }
        else -> null
    }
}
//
//fun MvPatBinding.asEntry(): SimpleScopeEntry =
//    SimpleScopeEntry(this.name, this, this.itemNs)
//
//fun MvModule.asEntry(): SimpleScopeEntry? {
//    val name = this.name ?: return null
//    return SimpleScopeEntry(name, this, this.itemNs)
//}
//
//fun MvFunctionLike.asEntry(): ScopeEntryWithVisibility? {
//    val name = this.name ?: return null
//    return ScopeEntryWithVisibility(
//        name,
//        this,
//        this.itemNs,
//        itemScopeAdjustment = NamedItemScope.MAIN
//    )
//}
//
//fun MvTypeParameter.asEntry(): SimpleScopeEntry? {
//    val name = this.name ?: return null
//    return SimpleScopeEntry(name, this, this.itemNs)
//}
//
//fun MvSchema.asEntry(): ScopeEntryWithVisibility? {
//    val name = this.name ?: return null
//    return ScopeEntryWithVisibility(
//        name,
//        this,
//        this.itemNs,
//        itemScopeAdjustment = NamedItemScope.MAIN
//    )
//}
//
//fun MvConst.asEntry(): ScopeEntryWithVisibility? {
//    val name = this.name ?: return null
//    return ScopeEntryWithVisibility(
//        name,
//        this,
//        this.itemNs,
//        itemScopeAdjustment = NamedItemScope.MAIN
//    )
//}
//
//fun MvGlobalVariableStmt.asEntry(): ScopeEntryWithVisibility? {
//    val name = this.name ?: return null
//    return ScopeEntryWithVisibility(
//        name,
//        this,
//        this.itemNs,
//        itemScopeAdjustment = NamedItemScope.MAIN
//    )
//}
//
//fun MvFieldDecl.asEntry(): SimpleScopeEntry? {
//    val name = this.name ?: return null
//    return SimpleScopeEntry(name, this, this.itemNs)
//}
//
//fun MvEnumVariant.asEntry(): SimpleScopeEntry? {
//    val name = this.name ?: return null
//    return SimpleScopeEntry(name, this, this.itemNs)
//}
//
//fun MvStruct.asEntry(): ScopeEntryWithVisibility? {
//    val name = this.name ?: return null
//    return ScopeEntryWithVisibility(
//        name,
//        this,
//        TYPES,
//        itemScopeAdjustment = NamedItemScope.MAIN
//    )
//}
//
//fun MvEnum.asEntry(): ScopeEntryWithVisibility? {
//    val name = this.name ?: return null
//    return ScopeEntryWithVisibility(
//        name,
//        this,
//        ENUMS,
//        itemScopeAdjustment = NamedItemScope.MAIN
//    )
//}
//
//fun MvLabelDecl.asEntry(): SimpleScopeEntry? {
//    val name = this.name ?: return null
//    return SimpleScopeEntry(name, this, NONE)
//}