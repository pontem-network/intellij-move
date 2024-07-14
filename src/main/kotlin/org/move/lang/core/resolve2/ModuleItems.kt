package org.move.lang.core.resolve2

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Visibility

private val MODULE_ITEMS_KEY: Key<CachedValue<List<VisItem>>> = Key.create("MODULE_ITEMS_KEY")

data class VisItem(
    val name: String,
    val item: MvNamedElement,
    val visibility: Visibility,
    val isFromNamedImport: Boolean
)

//val MvItemsOwner.visibleItems: List<VisItem>
//    get() =
//        CachedValuesManager.getCachedValue(this, MODULE_ITEMS_KEY) {
//            val declaredItems = buildList<MvNamedElement> {
//                if (this is MvModule) {
//                    addAll(allNonTestFunctions())
//                    addAll(consts())
//                    addAll(structs())
//                    addAll(schemas())
//                }
//            }
//                .mapNotNull {
//                    val visibility = (it as? MvItemElement)?.visibility2 ?: Visibility.Internal
//                    it.name?.let { name ->
//                        VisItem(name, it, visibility, false)
//                    }
//                }
//
//            val speckItems = this.useSpeckVisibleItems
//
//            CachedValueProvider.Result.create(
//                declaredItems + speckItems,
//                listOf(moveStructureOrAnyPsiModificationTracker)
//            )
//        }

val MvModule.declaredItems: List<MvItemElement>
    get() {
        return buildList {
            addAll(allNonTestFunctions())
            addAll(consts())
            addAll(structs())
            addAll(schemas())
        }
    }

//val MvItemsOwner.useSpeckVisibleItems: List<VisItem>
//    get() {
//        val visibleItems = mutableListOf<VisItem>()
//        for (useStmt in this.useStmtList) {
//            useStmt.forEachLeafSpeck { basePath, speckPath, alias ->
//                val qualifier = basePath ?: speckPath.path
//                if (qualifier == null) {
//                    // in a form of `use NAME;`, invalid
//                    return@forEachLeafSpeck
//                }
//
//
////                val ctx = PathResolutionContext(speckPath, ContextScopeInfo.from(speckPath))
////                processQualifiedPathResolveVariants(
////                    ctx,
////                    Namespace.all(),
////                    speckPath,
////                    qualifier,
////                    createStoppableProcessor { e ->
////                        val visItem = VisItem(alias ?: e.name, e.element, )
////                    })
////            val qualifierModule = qualifier.reference?.resolve() as? MvModule ?: return@forEachLeafSpeck
////            processItemDeclarations(qualifierModule, Namespace.all()) {
////
////            }
////            val visItem = VisItem(
////                resolvedPath,
////                visibility = Visibility.Internal,
////                isFromNamedImport = true)
////            visibleItems.add(visItem)
//            }
//        }
//        return visibleItems
//    }
