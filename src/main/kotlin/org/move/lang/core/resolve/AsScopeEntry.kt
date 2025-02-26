package org.move.lang.core.resolve

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvFieldDecl
import org.move.lang.core.resolve.ref.*

fun MvPatBinding.asEntry(): SimpleScopeEntry =
    SimpleScopeEntry(this.name, this, this.itemNs)

fun MvModule.asEntry(): SimpleScopeEntry? {
    val name = this.name ?: return null
    return SimpleScopeEntry(name, this, this.itemNs)
}

fun MvFunctionLike.asEntry(): ScopeEntryWithVisibility? {
    val name = this.name ?: return null
    return ScopeEntryWithVisibility(
        name,
        this,
        this.itemNs,
        itemScope = NamedItemScope.MAIN
    )
}

fun MvTypeParameter.asEntry(): SimpleScopeEntry? {
    val name = this.name ?: return null
    return SimpleScopeEntry(name, this, this.itemNs)
}

fun MvSchema.asEntry(): ScopeEntryWithVisibility? {
    val name = this.name ?: return null
    return ScopeEntryWithVisibility(
        name,
        this,
        this.itemNs,
        itemScope = NamedItemScope.MAIN
    )
}

fun MvConst.asEntry(): ScopeEntryWithVisibility? {
    val name = this.name ?: return null
    return ScopeEntryWithVisibility(
        name,
        this,
        this.itemNs,
        itemScope = NamedItemScope.MAIN
    )
}

fun MvGlobalVariableStmt.asEntry(): ScopeEntryWithVisibility? {
    val name = this.name ?: return null
    return ScopeEntryWithVisibility(
        name,
        this,
        this.itemNs,
        itemScope = NamedItemScope.MAIN
    )
}

fun MvFieldDecl.asEntry(): SimpleScopeEntry? {
    val name = this.name ?: return null
    return SimpleScopeEntry(name, this, this.itemNs)
}

fun MvEnumVariant.asEntry(): SimpleScopeEntry? {
    val name = this.name ?: return null
    return SimpleScopeEntry(name, this, this.itemNs)
}

fun MvStruct.asEntry(): ScopeEntryWithVisibility? {
    val name = this.name ?: return null
    return ScopeEntryWithVisibility(
        name,
        this,
        TYPES,
        itemScope = NamedItemScope.MAIN
    )
}

fun MvEnum.asEntry(): ScopeEntryWithVisibility? {
    val name = this.name ?: return null
    return ScopeEntryWithVisibility(
        name,
        this,
        ENUMS,
        itemScope = NamedItemScope.MAIN
    )
}

fun MvLabelDecl.asEntry(): SimpleScopeEntry? {
    val name = this.name ?: return null
    return SimpleScopeEntry(name, this, NONE)
}