package org.move.ide.utils.imports

import org.move.ide.inspections.imports.UseItemType.MODULE
import org.move.ide.inspections.imports.useItems
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
    val insertionScope = context.containingModule?.moduleBlock
        ?: context.containingScript?.scriptBlock
        ?: return
    val insertTestOnly =
        insertionScope.itemScope == NamedItemScope.MAIN
                && context.itemScope == NamedItemScope.TEST
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
    usePath: ItemQualName,
    testOnly: Boolean
): Boolean {
    if (usePath.moduleName == null) return false
    val psiFactory = mod.project.psiFactory
    val useStmts = mod.useStmtList.filter { it.hasTestOnlyAttr == testOnly }
    return useStmts
//        .mapNotNull { it.itemUseSpeck }
        .any { tryGroupWithItemSpeck(psiFactory, it, usePath) }
}

private fun tryGroupWithItemSpeck(
    psiFactory: MvPsiFactory, useStmt: MvUseStmt, usePath: ItemQualName
): Boolean {
    // module imports do not support groups for now
    val useItems = useStmt.useItems
    if (useItems.all { it.type == MODULE }) return false

    val itemUseItem = useItems.firstOrNull() ?: return false
    val itemUseSpeck = itemUseItem.useSpeck
    val qualifier = itemUseSpeck.qualifier ?: itemUseSpeck.path.qualifier ?: return false

    val fqModuleName = usePath.editorModuleFqName() ?: error("checked in the upper level")
    if (!qualifier.textMatches(fqModuleName)) return false

    val itemName = usePath.itemName
    val useStmtNames = useItems.map { it.nameOrAlias }
    if (itemName in useStmtNames) return true

    val useGroup = itemUseSpeck.useGroup
    val useSpeck = psiFactory.useSpeckForGroup(itemName)
    if (useGroup != null) {
        // add after the last item
        val useSpeckList = useGroup.useSpeckList
        if (useSpeckList.isEmpty()) {
            // use 0x1::m::{};
            useGroup.addAfter(useSpeck, useGroup.lBrace)
        } else {
            // use 0x1::m::{item1} -> use 0x1::m::{item1, item2}
            val lastItem = useSpeckList.last()
            useGroup.addAfter(
                useSpeck,
                useGroup.addAfter(psiFactory.createComma(), lastItem)
            )
        }
    } else {
        // 0x1::dummy::{}
        val newRootUseSpeck = psiFactory.useSpeckWithEmptyUseGroup()

        // 0x1::dummy::{} -> 0x1::m::{}
        newRootUseSpeck.path.replace(qualifier)

        // 0x1::m::{} -> 0x1::m::{item}
        val existingItemName = itemUseSpeck.path.referenceName ?: return false
        val newItemUseSpeck = psiFactory.useSpeckForGroup(existingItemName)
        val newUseGroup = newRootUseSpeck.useGroup ?: error("created with use group")
        newUseGroup.addAfter(newItemUseSpeck, newUseGroup.lBrace)

        // 0x1::m::{item} -> 0x1::m::{item as myitem}
        val existingUseAlias = itemUseSpeck.useAlias
        if (existingUseAlias != null) {
            newItemUseSpeck.add(existingUseAlias)
        }

        // 0x1::m::{item as myitem} -> 0x1::m::{item as myitem, }
        val comma = newUseGroup.addBefore(psiFactory.createComma(), newUseGroup.rBrace)

        // 0x1::m::{item as myitem, } -> 0x1::m::{item as myitem, item2}
        newUseGroup.addAfter(useSpeck, comma)

        newRootUseSpeck.useGroup?.replace(newUseGroup)
        useStmt.useSpeck?.replace(newRootUseSpeck)
    }
    return true
}

private val <T : MvElement> List<T>.lastElement: T? get() = maxByOrNull { it.textOffset }

private fun insertUseStmtAtTheCorrectLocation(mod: MvItemsOwner, useStmt: MvUseStmt): Boolean {
    val psiFactory = MvPsiFactory(mod.project)
    val newline = psiFactory.createNewline()
    val useStmts = mod.childrenOfType<MvUseStmt>().map(::UseStmtWrapper)
    if (useStmts.isEmpty()) {
        val anchor = mod.items().first()
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
