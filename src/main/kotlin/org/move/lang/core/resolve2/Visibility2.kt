package org.move.lang.core.resolve2

import org.move.ide.inspections.imports.pathUsageScope
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ModInfo
import org.move.lang.core.resolve.VisibilityFilter
import org.move.lang.core.resolve.VisibilityStatus.Invisible
import org.move.lang.core.resolve.VisibilityStatus.Visible
import org.move.lang.core.resolve.ref.Namespace.CONST
import org.move.lang.core.resolve.ref.Namespace.TYPE
import org.move.lang.core.resolve.ref.Visibility2
import org.move.lang.core.resolve.ref.Visibility2.*

data class ItemVisibility(
    val item: MvItemElement,
    val isTestOnly: Boolean,
    val vis: Visibility2,
)

/** Creates filter which determines whether item with [this] visibility is visible from specific [ModInfo] */
fun ItemVisibility.createFilter(): VisibilityFilter {
    val (item, isTestOnly, vis) = this
    return VisibilityFilter { path, namespaces ->

        // inside msl
        if (path.isMsl()) return@VisibilityFilter Visible

        // types are always visible
        if (namespaces.contains(TYPE)) return@VisibilityFilter Visible

        // if inside MvAttrItem like abort_code=
        val attrItem = path.ancestorStrict<MvAttrItem>()
        if (attrItem != null) return@VisibilityFilter Visible

        val pathUsageScope = path.pathUsageScope
        val useSpeck = path.ancestorStrict<MvUseSpeck>()
        if (useSpeck != null) {
            // inside import, all visibilities except for private work
            if (vis is Restricted) return@VisibilityFilter Visible

            // consts are importable in tests
            if (pathUsageScope.isTest && namespaces.contains(CONST)) return@VisibilityFilter Visible
        }

        // #[test_only] items in non-test-only scope
        if (isTestOnly && !pathUsageScope.isTest) return@VisibilityFilter Invisible

        // Self::method
        val itemModule = item.containingModule
        val pathModule = path.containingModule
        if (itemModule == pathModule) return@VisibilityFilter Visible

        when (vis) {
            is Restricted -> {
                when (vis) {
                    is Restricted.Friend -> {
                        val friends = vis.friendModules
                        val modInfo = pathModule?.fqModule()
                        if (modInfo in friends) Visible else Invisible
                    }
                    is Restricted.Script -> {
                        val containingFunction = path.containingFunction
                        if (containingFunction != null) {
                            if (containingFunction.isEntry
                                || containingFunction.isPublicScript
                            ) return@VisibilityFilter Visible
                        }
                        if (path.containingScript != null) return@VisibilityFilter Visible
                        Invisible
                    }
                    is Restricted.Package -> {
                        // todo
                        Visible
                    }
                }
            }
            is Public -> Visible
            is Private -> Invisible
        }
    }
}