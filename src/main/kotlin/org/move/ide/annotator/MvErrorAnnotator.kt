package org.move.ide.annotator

import com.intellij.ide.plugins.PluginManager
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvFunctionDef
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.MvNamedElement

class MvErrorAnnotator : AnnotatorBase() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val visitor = object : MvVisitor() {
            override fun visitFunctionDef(o: MvFunctionDef) = checkFunctionDef(holder, o)
        }
        element.accept(visitor)
    }

    private fun checkFunctionDef(holder: AnnotationHolder, element: MvFunctionDef) {
        val module = element.parent

        val duplicateFunctionNames = module
            .namedChildren()
            .groupBy { it.name }
            .map { it.value }
            .filter { it.size > 1 }
            .flatten()
            .toSet()
        if (element.name !in duplicateFunctionNames.map { it.name }) {
            return
        }
        val builder = holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate definitions with name `${element.name}`")
        builder.range(element)
        builder.create()
    }
}

//private fun checkDuplicates(
//    holder: AnnotationHolder,
//    element: MvNameIdentifierOwner,
//    scope: PsiElement = element.parent,
//    recursively: Boolean = false
//) {
//    val duplicates = holder.currentAnnotationSession.duplicatesByNamespace(scope, recursively)
//    val ns = element.namespaces.find { element in duplicates[it].orEmpty() }
//        ?: return
//    val name = element.name!!
//
//    val identifier = element.nameIdentifier ?: element
//    val message = when {
//        element is RsNamedFieldDecl -> RsDiagnostic.DuplicateFieldError(identifier, name)
//        element is RsEnumVariant -> RsDiagnostic.DuplicateEnumVariantError(identifier, name)
//        element is RsLifetimeParameter -> RsDiagnostic.DuplicateLifetimeError(identifier, name)
//        element is RsPatBinding && owner is RsValueParameterList -> RsDiagnostic.DuplicateBindingError(identifier, name)
//        element is RsTypeParameter -> RsDiagnostic.DuplicateTypeParameterError(identifier, name)
//        owner is RsImplItem -> RsDiagnostic.DuplicateDefinitionError(identifier, name)
//        else -> {
//            val scopeType = when (owner) {
//                is RsBlock -> "block"
//                is RsMod, is RsForeignModItem -> "module"
//                is RsTraitItem -> "trait"
//                else -> "scope"
//            }
//            RsDiagnostic.DuplicateItemError(identifier, ns.itemName, name, scopeType)
//        }
//    }
//    message.addToHolder(holder)
//}

//private fun AnnotationSession.duplicatesByNamespace(
//    owner: PsiElement,
//    recursively: Boolean
//): Set<PsiElement> {
//
//}
//
//private fun AnnotationSession.duplicatesByNamespace(
//    owner: PsiElement,
//    recursively: Boolean
//): Map<Namespace, Set<PsiElement>> {
//    if (owner.parent is RsFnPointerType) return emptyMap()
//
//    fun PsiElement.namespaced(): Sequence<Pair<Namespace, PsiElement>> =
//        when (this) {
//            is RsNamedElement -> namespaces
//            is RsUseSpeck -> namespaces
//            else -> emptySet()
//        }.asSequence().map { Pair(it, this) }
//
//    val fileMap = fileDuplicatesMap()
//    fileMap[owner]?.let { return it }
//
//    val importedNames = (owner as? RsItemsOwner)
//        ?.expandedItemsCached
//        ?.namedImports
//        ?.asSequence()
//        ?.mapNotNull { it.path.parent as? RsUseSpeck }
//        .orEmpty()
//    val duplicates: Map<Namespace, Set<PsiElement>> =
//        (owner.namedChildren(recursively, stopAt = RsFnPointerType::class.java)
//                + importedNames)
//            .filter { it !is RsExternCrateItem } // extern crates can have aliases.
//            .filter { it.nameOrImportedName() != null }
//            .filter { it !is RsDocAndAttributeOwner || (it.isEnabledByCfg && !it.isCfgUnknown) }
//            .flatMap { it.namespaced() }
//            .groupBy { it.first }       // Group by namespace
//            .map { entry ->
//                val (namespace, items) = entry
//                namespace to items.asSequence()
//                    .map { it.second }
//                    .groupBy { it.nameOrImportedName() }
//                    .map { it.value }
//                    .filter { it.size > 1 }
//                    .flatten()
//                    .toSet()
//            }
//            .toMap()
//
//    fileMap[owner] = duplicates
//    return duplicates
//}

private fun PsiElement.namedChildren(): Sequence<MvNamedElement> {
    return this.children.filterIsInstance<MvNamedElement>().asSequence()
}
