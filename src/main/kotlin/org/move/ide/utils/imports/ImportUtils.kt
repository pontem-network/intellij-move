package org.move.ide.utils.imports

import org.move.ide.inspections.imports.usageScope
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ItemQualName
import org.move.openapiext.checkWriteAccessAllowed

/**
 * Inserts a use declaration to the mod where [context] located for importing the selected candidate ([this]).
 * This action requires write access.
 */
fun ImportCandidate.import(context: MvElement) {
    checkWriteAccessAllowed()
    val insertionScope = context.containingModule ?: context.containingScript ?: return
    val insertTestOnly =
        insertionScope.usageScope == NamedItemScope.MAIN
                && context.usageScope == NamedItemScope.TEST
    insertionScope.insertUseItem(qualName, insertTestOnly)
}

private fun MvItemsOwner.insertUseItem(usePath: ItemQualName, testOnly: Boolean) {

    if (tryInsertingIntoExistingUseStmt(this, usePath, testOnly)) return

    val newUseStmt =
        this.project.psiFactory.useStmt(usePath.editorText(), testOnly)
    insertUseStmtAtTheCorrectLocation(this, newUseStmt)

//    val anchor = childrenOfType<MvUseStmt>().lastElement
//    if (anchor != null) {
//        addAfter(newUseStmt, anchor)
//    } else {
//        val firstItem = this.items().first()
//        addBefore(newUseStmt, firstItem)
//        addBefore(psiFactory.createNewline(), firstItem)
//    }
}

private fun tryInsertingIntoExistingUseStmt(
    mod: MvItemsOwner,
    itemQualName: ItemQualName,
    testOnly: Boolean
): Boolean {
    if (itemQualName.moduleName == null) return false
    val psiFactory = mod.project.psiFactory
    val useStmts = mod.useStmtList.filter { it.hasTestOnlyAttr == testOnly }
    return useStmts
        .any { tryGroupWithItemSpeck(psiFactory, it, itemQualName) }
}

private fun tryGroupWithItemSpeck(
    psiFactory: MvPsiFactory, useStmt: MvUseStmt, itemQualName: ItemQualName
): Boolean {
    val rootUseSpeck = useStmt.useSpeck ?: return false

    val useGroup = rootUseSpeck.useGroup
    // 0x1::m -> module imports does not support groups, can't insert
    if (useGroup == null && rootUseSpeck.path.length < 3) return false

    // searching for the statement with the same module qualifier
    val itemModulePath = itemQualName.editorModuleFqName() ?: error("moduleName cannot be zero")
    if (useGroup == null) {
        val modulePath = rootUseSpeck.path.qualifier ?: return false
        if (!modulePath.textMatches(itemModulePath)) return false
    } else {
        val modulePath = rootUseSpeck.path
        if (!modulePath.textMatches(itemModulePath)) return false
    }

    val itemName = itemQualName.itemName
    val newUseSpeck = psiFactory.useSpeckForGroup(itemName)

    if (useGroup == null) {
        // 0x1::dummy::{}
        val useSpeckWithGroup = psiFactory.useSpeckWithEmptyUseGroup()

        // [0x1::m]::item
        //  ^ qualifier
        val qualifier = rootUseSpeck.path.qualifier ?: return false
        // 0x1::dummy::{} -> 0x1::m::{}
        useSpeckWithGroup.path.replace(qualifier)

        // 0x1::m::{} -> 0x1::m::{item as dummy}
        val existingItemName = rootUseSpeck.path.referenceName ?: return false
        val groupItemUseSpeck = psiFactory.useSpeckForGroupWithDummyAlias(existingItemName)

        // 0x1::m::{item as dummy} -> 0x1::m::{item as myitem}
        val existingUseAlias = rootUseSpeck.useAlias
        if (existingUseAlias != null) {
            groupItemUseSpeck.useAlias?.replace(existingUseAlias)
        } else {
            groupItemUseSpeck.useAlias?.delete()
        }

        val newUseGroup = useSpeckWithGroup.useGroup!!
        newUseGroup.addAfter(groupItemUseSpeck, newUseGroup.lBrace)

        // 0x1::m::{item as myitem} -> 0x1::m::{item as myitem, }
        val comma = newUseGroup.addBefore(psiFactory.createComma(), newUseGroup.rBrace)

        // 0x1::m::{item as myitem, } -> 0x1::m::{item as myitem, item2}
        newUseGroup.addAfter(newUseSpeck, comma)

        useSpeckWithGroup.useGroup?.replace(newUseGroup)
        useStmt.useSpeck?.replace(useSpeckWithGroup)
    } else {
        // add after the last item
        val useSpeckList = useGroup.useSpeckList
        if (useSpeckList.isEmpty()) {
            // use 0x1::m::{};
            useGroup.addAfter(newUseSpeck, useGroup.lBrace)
        } else {
            // use 0x1::m::{item1} -> use 0x1::m::{item1, item2}
            val lastItem = useSpeckList.last()
            val newComma = useGroup.addAfter(psiFactory.createComma(), lastItem)
            useGroup.addAfter(newUseSpeck, newComma)
        }
    }

//    val useItems = useStmt.useItems
//    if (useItems.all { it.type == MODULE }) return false
//
//    val itemUseItem = useItems.firstOrNull() ?: return false
//    val itemUseSpeck = itemUseItem.useSpeck
//    val qualifier = itemUseSpeck.qualifier ?: itemUseSpeck.path.qualifier ?: return false
//
//    val fqModuleName = usePath.editorModuleFqName() ?: error("checked in the upper level")
//    if (!qualifier.textMatches(fqModuleName)) return false

//    val itemName = usePath.itemName
//    val useStmtNames = useItems.map { it.nameOrAlias }
//    if (itemName in useStmtNames) return true

//    val useGroup = itemUseSpeck.useGroup
//    val useSpeck = psiFactory.useSpeckForGroup(itemName)
//    if (useGroup != null) {
//        // add after the last item
//        val useSpeckList = useGroup.useSpeckList
//        if (useSpeckList.isEmpty()) {
//            // use 0x1::m::{};
//            useGroup.addAfter(useSpeck, useGroup.lBrace)
//        } else {
//            // use 0x1::m::{item1} -> use 0x1::m::{item1, item2}
//            val lastItem = useSpeckList.last()
//            useGroup.addAfter(
//                useSpeck,
//                useGroup.addAfter(psiFactory.createComma(), lastItem)
//            )
//        }
//    } else {
//    }
    return true
}

private val <T: MvElement> List<T>.lastElement: T? get() = maxByOrNull { it.textOffset }

@Suppress("SameReturnValue")
private fun insertUseStmtAtTheCorrectLocation(mod: MvItemsOwner, useStmt: MvUseStmt): Boolean {
    val psiFactory = MvPsiFactory(mod.project)
    val newline = psiFactory.newline()
    val useStmts = mod.childrenOfType<MvUseStmt>().map(::UseStmtWrapper)
    if (useStmts.isEmpty()) {
        val anchor = mod.firstItem
        mod.addAfter(newline, mod.addBefore(useStmt, anchor))
        return true
    }

    val useWrapper = UseStmtWrapper(useStmt)
    val (less, greater) = useStmts.partition { it < useWrapper }
    val anchorBefore = less.lastOrNull()
    val anchorAfter = greater.firstOrNull()
    when {
        anchorBefore != null -> {
            val addedItem = mod.addAfter(useStmt, anchorBefore.useStmt)
            mod.addBefore(newline, addedItem)
        }
        anchorAfter != null -> {
            val addedItem = mod.addBefore(useStmt, anchorAfter.useStmt)
            mod.addAfter(newline, addedItem)
        }
        else -> error("unreachable")
    }
    return true
}
