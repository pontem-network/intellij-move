package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNamedElementImpl
import org.move.lang.core.resolve.ContextScopeInfo
import org.move.lang.core.resolve.LetStmtScope
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.resolveModuleItem

val MvUseItem.itemUseSpeck: MvItemUseSpeck
    get() = ancestorStrict() ?: error("MvUseItem outside MvItemUseSpeck")

val MvUseItem.annotationItem: MvElement
    get() {
        val parent = this.parent
        if (parent is MvUseItemGroup && parent.useItemList.size != 1) return this
        return useStmt
    }

val MvUseItem.useStmt: MvUseStmt
    get() =
        ancestorStrict() ?: error("always has MvUseStmt as ancestor")

val MvUseItem.nameOrAlias: String?
    get() {
        val alias = this.useAlias
        if (alias != null) {
            return alias.identifier?.text
        }
        return this.identifier.text
    }

val MvUseItem.moduleName: String
    get() {
        val useStmt = this.ancestorStrict<MvUseStmt>()
        return useStmt?.itemUseSpeck?.fqModuleRef?.referenceName.orEmpty()
    }

val MvUseItem.isSelf: Boolean get() = this.identifier.textMatches("Self")
val MvUseSpeck.isSelf: Boolean get() = this.path.identifier?.textMatches("Self") ?: false

class MvUseItemReferenceElement(
    element: MvUseItem
): MvPolyVariantReferenceCached<MvUseItem>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        val fqModuleRef = element.itemUseSpeck.fqModuleRef
        val module =
            fqModuleRef.reference?.resolve() as? MvModule ?: return emptyList()
        if ((element.useAlias == null && element.text == "Self")
            || (element.useAlias != null && element.text.startsWith("Self as"))
        ) {
            return listOf(module)
        }

        val ns = setOf(
            Namespace.TYPE,
            Namespace.NAME,
            Namespace.FUNCTION,
            Namespace.SCHEMA,
            Namespace.CONST
        )
        val vs = Visibility.visibilityScopesForElement(fqModuleRef)

        // import has MAIN+VERIFY, and TEST if it or any of the parents has test
        val useItemScopes = mutableSetOf(NamedItemScope.MAIN, NamedItemScope.VERIFY)

        // gather scopes for all parents up to MvUseStmt
        var scopedElement: MvElement? = element
        while (scopedElement != null) {
            useItemScopes.addAll(scopedElement.itemScopes)
            scopedElement = scopedElement.parent as? MvElement
        }

        val contextScopeInfo =
            ContextScopeInfo(
                letStmtScope = LetStmtScope.EXPR_STMT,
                refItemScopes = useItemScopes,
            )
        return resolveModuleItem(
            module,
            element.referenceName,
            ns,
            vs,
            contextScopeInfo
        )
    }

}

abstract class MvUseItemMixin(node: ASTNode): MvNamedElementImpl(node),
                                              MvUseItem {
    override fun getName(): String? {
        val name = super.getName()
        if (name != "Self") return name
        return ancestorStrict<MvItemUseSpeck>()?.fqModuleRef?.referenceName ?: name
    }

    override val referenceNameElement: PsiElement get() = identifier

    override fun getReference() = MvUseItemReferenceElement(this)
}
