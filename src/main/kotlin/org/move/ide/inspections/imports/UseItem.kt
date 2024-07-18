package org.move.ide.inspections.imports

import org.move.ide.inspections.imports.UseItemType.*
import org.move.lang.core.psi.MvUseSpeck
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.psi.ext.MvItemsOwner
import org.move.lang.core.resolve2.PathKind
import org.move.lang.core.resolve2.pathKind
import org.move.lang.core.resolve2.util.forEachLeafSpeck

enum class UseItemType {
    MODULE, SELF_MODULE, ITEM;
}

data class UseItem(
    val useSpeck: MvUseSpeck,
    val nameOrAlias: String,
    val type: UseItemType,
    val scope: NamedItemScope
)

val MvItemsOwner.useItems: List<UseItem>
    get() = this.useStmtList.flatMap { it.useItems }

val MvUseStmt.useItems: List<UseItem>
    get() {
        val items = mutableListOf<UseItem>()
        val stmtItemScope = this.declaredItemScope
        this.forEachLeafSpeck { path, useAlias ->
            val useSpeck = path.parent as MvUseSpeck
            val nameOrAlias = useAlias?.name ?: path.referenceName ?: return@forEachLeafSpeck false
            val pathKind = path.pathKind()
            when (pathKind) {
                is PathKind.QualifiedPath.Module ->
                    items.add(UseItem(useSpeck, nameOrAlias, MODULE, stmtItemScope))
//                is ModulePath -> {
//                    items.add(UseItem(useSpeck, nameOrAlias, MODULE, stmtItemScope))
//                }
                is PathKind.QualifiedPath.ModuleItem -> {
                    // cannot be
                    return@forEachLeafSpeck false
                }
                is PathKind.QualifiedPath -> {
                    if (pathKind.path.referenceName == "Self") {
                        val moduleName =
                            useAlias?.name ?: pathKind.qualifier.referenceName ?: return@forEachLeafSpeck false
                        items.add(UseItem(useSpeck, moduleName, SELF_MODULE, stmtItemScope))
                    } else {
                        items.add(UseItem(useSpeck, nameOrAlias, ITEM, stmtItemScope))
                    }
                }
//                is QualifiedPath -> {
//                    if (kind.path.referenceName == "Self") {
//                        val moduleName =
//                            useAlias?.name ?: kind.qualifier.referenceName ?: return@forEachLeafSpeck false
//                        items.add(UseItem(useSpeck, moduleName, SELF_MODULE, stmtItemScope))
//                    } else {
//                        items.add(UseItem(useSpeck, nameOrAlias, ITEM, stmtItemScope))
//                    }
//                }
                else -> return@forEachLeafSpeck false
            }
            false
        }

        return items
    }
