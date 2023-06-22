package org.move.ide.utils.imports

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.childrenOfType
import org.move.lang.core.psi.ext.isTestOnly
import org.move.lang.core.psi.ext.names
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
        insertionScope.itemScope == ItemScope.MAIN
                && context.itemScope == ItemScope.TEST
    insertionScope.insertUseItem(qualName, insertTestOnly)
}

private fun MvImportsOwner.insertUseItem(usePath: ItemQualName, testOnly: Boolean) {

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
    mod: MvImportsOwner,
    usePath: ItemQualName,
    testOnly: Boolean
): Boolean {
    if (usePath.moduleName == null) return false
    val psiFactory = mod.project.psiFactory
    return mod
        .useStmtList
        .filter { it.isTestOnly == testOnly }
        .mapNotNull { it.itemUseSpeck }
        .any { tryGroupWithItemSpeck(psiFactory, it, usePath) }
}

private fun tryGroupWithItemSpeck(
    psiFactory: MvPsiFactory, itemUseSpeck: MvItemUseSpeck, usePath: ItemQualName
): Boolean {
    val fqModuleName = usePath.editorModuleFqName() ?: error("checked in the upper level")
    if (!itemUseSpeck.fqModuleRef.textMatches(fqModuleName)) return false

    val itemName = usePath.itemName
    if (itemName in itemUseSpeck.names()) return true

    val useItem = psiFactory.useItem(itemName)
    val useItemGroup = itemUseSpeck.useItemGroup
    if (useItemGroup != null) {
        // add after the last item
        val itemList = useItemGroup.useItemList
        if (itemList.isEmpty()) {
            // use 0x1::m::{};
            useItemGroup.addAfter(useItem, useItemGroup.lBrace)
        } else {
            // use 0x1::m::{item1} -> use 0x1::m::{item1, item2}
            val lastItem = itemList.last()
            useItemGroup.addAfter(
                useItem,
                useItemGroup.addAfter(psiFactory.createComma(), lastItem)
            )
        }
    } else {
        val existingItem = itemUseSpeck.useItem ?: return true

        val existingItemCopy = existingItem.copy()
        val itemGroup = existingItem.replace(psiFactory.useItemGroup(listOf())) as MvUseItemGroup

        val comma = itemGroup.addAfter(
            psiFactory.createComma(),
            itemGroup.addAfter(existingItemCopy, itemGroup.lBrace)
        )
        itemGroup.addAfter(useItem, comma)
    }
    return true
}

private val <T : MvElement> List<T>.lastElement: T? get() = maxByOrNull { it.textOffset }

private fun insertUseStmtAtTheCorrectLocation(mod: MvImportsOwner, useStmt: MvUseStmt): Boolean {
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
