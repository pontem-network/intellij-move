package org.move.lang.core.resolve

import org.move.cli.containingMovePackage
import org.move.cli.settings.moveSettings
import org.move.ide.inspections.imports.usageScope
import org.move.lang.core.psi.*
import org.move.lang.core.psi.NamedItemScope.MAIN
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.resolve.ref.Ns.*
import org.move.lang.core.resolve.ref.Visibility.*
import org.move.stdext.containsAny

fun isVisibleInContext(scopeEntry: ScopeEntry, contextElement: MvElement): Boolean {
    // inside msl everything is visible
    if (contextElement.isMsl()) return true

    // if inside MvAttrItem like abort_code=
    val attrItem = contextElement.ancestorStrict<MvAttrItem>()
    if (attrItem != null) return true

    val item = scopeEntry.element() ?: return false
    val itemNs = scopeEntry.namespaces

    val contextUsageScope = contextElement.usageScope
    val path = contextElement as? MvPath
    if (path != null) {
        val useSpeck = path.useSpeck
        if (useSpeck != null) {
            // for use specks, items needs to be public to be visible, no other rules apply
            if (item is MvModule || (item is MvItemElement && item.isPublic)) return true

            // msl-only items are available from imports
            if (item.isMslOnlyItem) return true

            // consts are importable in tests
            if (contextUsageScope.isTest && itemNs.contains(NAME)) return true
        }
    }

    // #[test] functions cannot be used from non-imports
    if (item is MvFunction && item.hasTestAttr) return false

    val itemModule = item.containingModule
    // 0x0::builtins module items are always visible
    if (itemModule != null && itemModule.isBuiltins) return true

    val itemUsageScope =
        if (scopeEntry.customItemScope != null) {
            item.usageScope.shrinkScope(scopeEntry.customItemScope)
        } else {
            item.usageScope
        }

    // #[test_only] items in non-test-only scope
    if (itemUsageScope != MAIN) {
        // cannot be used everywhere, need to check for scope compatibility
        if (itemUsageScope != contextUsageScope) return false
    }

    // we're in non-msl scope at this point, msl only items aren't available
    if (item is MslOnlyElement) return false

    val pathModule = contextElement.containingModule
    // local methods, Self::method - everything is visible
    if (itemModule == pathModule) return true

    // item is type, check whether it's allowed in the context
    if (itemNs.containsAny(TYPE, ENUM)) {
        val pathParent = path?.rootPath()?.parent
        when (pathParent) {
            // todo: when structs and enums can be public, conditions for struct lit/pat should be added here
            is MvPathType -> return true
        }
    }

    val visibility = (item as? MvVisibilityOwner)?.visibility ?: Public
    return when (visibility) {
        is Restricted -> {
            when (visibility) {
                is Restricted.Friend -> {
                    if (pathModule != null && itemModule != null) {
                        val friendModules = itemModule.friendModules
                        if (friendModules.any { isModulesEqual(it, pathModule) }) {
                            return true
                        }
                    }
                    false
                }
                is Restricted.Package -> {
                    if (!item.project.moveSettings.enablePublicPackage) {
                        return false
                    }
                    val pathPackage =
                        contextElement.containingMovePackage ?: return false
                    val itemPackage = item.containingMovePackage ?: return false
                    pathPackage == itemPackage
                }
            }
        }
        is Public -> true
        is Private -> false
    }
}
