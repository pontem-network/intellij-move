package org.move.lang.core.resolve

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvFieldDecl
import org.move.lang.core.resolve.ref.*

fun MvPatBinding.asEntry(): SimpleScopeEntry =
    SimpleScopeEntry(this.name, this, NAMES)

fun MvModule.asEntry(): SimpleScopeEntry? {
    val name = this.name ?: return null
    return SimpleScopeEntry(name, this, MODULES)
}

fun MvFunctionLike.asEntry(): ScopeEntryWithVisibility? {
    val name = this.name ?: return null
    return ScopeEntryWithVisibility(
        name,
        this,
        NAMES,
        adjustedItemScope = NamedItemScope.MAIN
    )
}

fun MvTypeParameter.asEntry(): SimpleScopeEntry? {
    val name = this.name ?: return null
    return SimpleScopeEntry(name, this, TYPES)
}

fun MvSchema.asEntry(): ScopeEntryWithVisibility? {
    val name = this.name ?: return null
    return ScopeEntryWithVisibility(
        name,
        this,
        SCHEMAS,
        adjustedItemScope = NamedItemScope.MAIN
    )
}

fun MvConst.asEntry(): ScopeEntryWithVisibility? {
    val name = this.name ?: return null
    return ScopeEntryWithVisibility(
        name,
        this,
        NAMES,
        adjustedItemScope = NamedItemScope.MAIN
    )
}

fun MvGlobalVariableStmt.asEntry(): ScopeEntryWithVisibility? {
    val name = this.name ?: return null
    return ScopeEntryWithVisibility(
        name,
        this,
        NAMES,
        adjustedItemScope = NamedItemScope.MAIN
    )
}

fun MvFieldDecl.asEntry(): SimpleScopeEntry? {
    val name = this.name ?: return null
    return SimpleScopeEntry(name, this, NAMES)
}

fun MvEnumVariant.asEntry(): SimpleScopeEntry? {
    val name = this.name ?: return null
    return SimpleScopeEntry(name, this, TYPES_N_NAMES)
}

fun MvStruct.asEntry(): ScopeEntryWithVisibility? {
    val name = this.name ?: return null
    return ScopeEntryWithVisibility(
        name,
        this,
        TYPES,
        adjustedItemScope = NamedItemScope.MAIN
    )
}

fun MvEnum.asEntry(): ScopeEntryWithVisibility? {
    val name = this.name ?: return null
    return ScopeEntryWithVisibility(
        name,
        this,
        ENUMS,
        adjustedItemScope = NamedItemScope.MAIN
    )
}

fun MvLabelDecl.asEntry(): SimpleScopeEntry? {
    val name = this.name ?: return null
    return SimpleScopeEntry(name, this, LABELS)
}