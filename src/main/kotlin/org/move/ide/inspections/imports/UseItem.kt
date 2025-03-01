package org.move.ide.inspections.imports

import org.move.cli.settings.debugError
import org.move.ide.inspections.imports.UseItemType.*
import org.move.lang.core.psi.MvUseSpeck
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.psi.ext.MvItemsOwner
import org.move.lang.core.resolve.PathKind
import org.move.lang.core.resolve.pathKind
import org.move.lang.core.resolve.scopeEntry.useSpeckLeaves

enum class UseItemType {
    MODULE, SELF_MODULE, ITEM;
}

data class UseItem(
    val useSpeck: MvUseSpeck,
    val nameOrAlias: String,
    val type: UseItemType,
    val scope: NamedItemScope
)

val MvItemsOwner.useItems: List<UseItem> get() = this.useStmtList.flatMap { it.useItems }

val MvUseStmt.useItems: List<UseItem>
    get() {
        val stmt = this
        val stmtScope = stmt.usageScope
        return buildList {
            for ((speckPath, speckAlias) in stmt.useSpeckLeaves) {
                val useSpeck = speckPath.parent as MvUseSpeck
                val nameOrAlias = speckAlias?.name ?: speckPath.referenceName ?: continue
                val pathKind = speckPath.pathKind()
                when (pathKind) {
                    is PathKind.QualifiedPath.Module ->
                        add(UseItem(useSpeck, nameOrAlias, MODULE, stmtScope))
                    is PathKind.QualifiedPath.ModuleItemOrEnumVariant -> {
                        debugError("not reachable, must be a bug")
                    }
                    is PathKind.QualifiedPath -> {
                        if (pathKind.path.referenceName == "Self") {
                            val moduleName =
                                speckAlias?.name ?: pathKind.qualifier.referenceName ?: continue
                            add(UseItem(useSpeck, moduleName, SELF_MODULE, stmtScope))
                        } else {
                            add(UseItem(useSpeck, nameOrAlias, ITEM, stmtScope))
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
