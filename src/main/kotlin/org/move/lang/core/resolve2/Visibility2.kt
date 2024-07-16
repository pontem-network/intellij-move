package org.move.lang.core.resolve2

import org.move.cli.containingMovePackage
import org.move.cli.settings.moveSettings
import org.move.ide.inspections.imports.pathUsageScope
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ModInfo
import org.move.lang.core.resolve.VisibilityFilter
import org.move.lang.core.resolve.VisibilityStatus.Invisible
import org.move.lang.core.resolve.VisibilityStatus.Visible
import org.move.lang.core.resolve.ref.Namespace.*
import org.move.lang.core.resolve.ref.Visibility2
import org.move.lang.core.resolve.ref.Visibility2.*

data class ItemVisibilityInfo(
    val item: MvItemElement,
    val isTestOnly: Boolean,
    val vis: Visibility2,
)

val MvItemElement.visInfo: ItemVisibilityInfo get() =
    ItemVisibilityInfo(this, isTestOnly = this.hasTestOnlyAttr, vis = this.visibility2)

/** Creates filter which determines whether item with [this] visibility is visible from specific [ModInfo] */
fun ItemVisibilityInfo.createFilter(): VisibilityFilter {
    val (item, isTestOnly, visibility) = this
    return VisibilityFilter { path, namespaces ->

        // inside msl everything is visible
        if (path.isMsl()) return@VisibilityFilter Visible

        // types are always visible, their correct usage is checked in a separate inspection
        if (namespaces.contains(TYPE)) return@VisibilityFilter Visible

        // if inside MvAttrItem like abort_code=
        val attrItem = path.ancestorStrict<MvAttrItem>()
        if (attrItem != null) return@VisibilityFilter Visible

        val pathUsageScope = path.pathUsageScope
        val useSpeck = path.useSpeck
        if (useSpeck != null) {
            // inside import, all visibilities except for private work
            if (visibility is Restricted) return@VisibilityFilter Visible

            // msl-only items are available from imports
            if (item.isMslOnlyItem) return@VisibilityFilter Visible

            // consts are importable in tests
            if (pathUsageScope.isTest && namespaces.contains(NAME)) return@VisibilityFilter Visible
        }

        // #[test_only] items in non-test-only scope
        if (isTestOnly && !pathUsageScope.isTest) return@VisibilityFilter Invisible

        // Self::method
        val itemModule = item.containingModule
        val pathModule = path.containingModule
        if (itemModule == pathModule) return@VisibilityFilter Visible

        when (visibility) {
            is Restricted -> {
                when (visibility) {
                    is Restricted.Friend -> {
                        val friends = visibility.friendModules
                        val modInfo = pathModule?.fqModule()
                        if (modInfo in friends) Visible else Invisible
                    }
                    is Restricted.Script -> {
                        val containingFunction = path.containingFunction
                        if (containingFunction != null) {
                            if (containingFunction.isEntry || containingFunction.isPublicScript
                            ) return@VisibilityFilter Visible
                        }
                        if (path.containingScript != null) return@VisibilityFilter Visible
                        Invisible
                    }
                    is Restricted.Package -> {
                        if (!item.project.moveSettings.enablePublicPackage) {
                            return@VisibilityFilter Invisible
                        }
                        val pathPackage = path.containingMovePackage ?: return@VisibilityFilter Invisible
                        if (visibility.originPackage == pathPackage) Visible else Invisible
                    }
                }
            }
            is Public -> Visible
            is Private -> Invisible
        }
    }
}