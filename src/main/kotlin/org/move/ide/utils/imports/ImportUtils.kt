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
    val psiFactory = element.project.psiFactory
    val insertionScope = context.containingModule?.moduleBlock
        ?: context.containingScript?.scriptBlock
        ?: return
    val insertTestOnly =
        insertionScope.itemScope == ItemScope.MAIN
                && context.itemScope == ItemScope.TEST
    insertionScope.insertUseItem(psiFactory, qualName, insertTestOnly)
}

private fun MvImportsOwner.insertUseItem(psiFactory: MvPsiFactory, usePath: ItemQualName, testOnly: Boolean) {
    val newUseStmt = psiFactory.useStmt(usePath.editorText(), testOnly)
    if (this.tryGroupWithOtherUseItems(psiFactory, newUseStmt, testOnly)) return

    val anchor = childrenOfType<MvUseStmt>().lastElement
    if (anchor != null) {
        addAfter(newUseStmt, anchor)
    } else {
        val firstItem = this.items().first()
        addBefore(newUseStmt, firstItem)
        addBefore(psiFactory.createNewline(), firstItem)
    }
}

private fun MvImportsOwner.tryGroupWithOtherUseItems(
    psiFactory: MvPsiFactory,
    newUseStmt: MvUseStmt,
    testOnly: Boolean
): Boolean {
    val newUseSpeck = newUseStmt.itemUseSpeck ?: return false
    val newName = newUseSpeck.names().singleOrNull() ?: return false
    val newFqModule = newUseSpeck.fqModuleRef
    return useStmtList
        .filter { it.isTestOnly == testOnly }
        .mapNotNull { it.itemUseSpeck }
        .any { it.tryGroupWith(psiFactory, newFqModule, newName) }
}

private fun MvItemUseSpeck.tryGroupWith(
    psiFactory: MvPsiFactory,
    newFqModule: MvFQModuleRef,
    newName: String
): Boolean {
    if (!this.fqModuleRef.textMatches(newFqModule)) return false
    if (newName in this.names()) return true
    val speck = psiFactory.itemUseSpeck(newFqModule.text, this.names() + newName)
    this.replace(speck)
    return true
}

private val <T : MvElement> List<T>.lastElement: T? get() = maxByOrNull { it.textOffset }
