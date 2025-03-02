package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.move.ide.MoveIcons
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvMandatoryNameIdentifierOwnerImpl
import org.move.lang.core.resolve.ref.MvBindingPatReferenceImpl
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.types.ty.Mutability
import javax.swing.Icon

// todo: replace with bindingTypeOwner later
val MvPatBinding.bindingOwner: PsiElement?
    get() = PsiTreeUtil.findFirstParent(this) {
        it is MvLetStmt
                || it is MvFunctionParameter
                || it is MvSchemaFieldStmt
                || it is MvLambdaParameter
                || it is MvConst
    }

val MvPatBinding.bindingTypeOwner: PsiElement?
    get() {
        var owner = this.parent
        if (owner is MvPat) {
            owner = this.findFirstParent { it is MvTypeAscriptionOwner }
        }
        return owner
    }

sealed class RsBindingModeKind {
    data object BindByValue : RsBindingModeKind()
    class BindByReference(val mutability: Mutability) : RsBindingModeKind()
}

//val MvPatBinding.kind get() = RsBindingModeKind.BindByValue

abstract class MvPatBindingMixin(node: ASTNode) : MvMandatoryNameIdentifierOwnerImpl(node),
                                                  MvPatBinding {

    // XXX: RsPatBinding is both a name element and a reference element:
    //
    // ```
    // match Some(82) {
    //     None => { /* None is a reference */ }
    //     Nope => { /* Nope is a named element*/ }
    // }
    // ```
    override fun getReference(): MvPolyVariantReference = MvBindingPatReferenceImpl(this)

    override val referenceNameElement: PsiElement get() = nameIdentifier
    override val referenceName: String get() = name

    override fun getIcon(flags: Int): Icon =
        when (this.bindingTypeOwner) {
            is MvFunctionParameter -> MoveIcons.PARAMETER
            is MvConst -> MoveIcons.CONST
            else -> MoveIcons.VARIABLE
        }

    override fun getUseScope(): SearchScope {
        return when (this.bindingTypeOwner) {
            is MvFunctionParameter -> {
                val function = this.ancestorStrict<MvFunction>() ?: return super.getUseScope()
                var combinedScope: SearchScope = LocalSearchScope(function)
                for (itemSpec in function.innerItemSpecs()) {
                    combinedScope = combinedScope.union(LocalSearchScope(itemSpec))
                }
                for (itemSpec in function.outerItemSpecs()) {
                    combinedScope = combinedScope.union(LocalSearchScope(itemSpec))
                }
                combinedScope
            }
            is MvLetStmt -> {
                val function = this.ancestorStrict<MvFunction>() ?: return super.getUseScope()
                LocalSearchScope(function)
            }
            is MvSchemaFieldStmt -> super.getUseScope()
            is MvConst -> super.getUseScope()
            else -> super.getUseScope()
        }
    }
}
