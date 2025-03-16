package org.move.lang.core.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.move.ide.presentation.text
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvFieldDecl
import org.move.lang.core.psi.ext.MvMethodOrField
import org.move.lang.core.psi.ext.addressRef
import org.move.lang.core.psi.ext.outerFileName
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.core.types.ty.functionTy
import org.move.utils.parametersSignatureText
import org.move.utils.returnTypeLookupText
import org.move.utils.signatureText

fun createCompletionItem(
    scopeEntry: ScopeEntry,
    completionContext: MvCompletionContext,
    applySubst: Substitution,
    priority: Double,
    insertHandler: InsertHandler<LookupElement> = DefaultInsertHandler(completionContext)
): CompletionItem? {
    val element = scopeEntry.element() ?: return null
    val lookup = element
        .getLookupElementBuilder(completionContext, scopeEntry.name, applySubst)
        .withInsertHandler(insertHandler)
        .withPriority(priority)
    val props = getLookupElementProperties(element, applySubst, completionContext)
    return lookup.toCompletionItem(properties = props)
}

fun MvNamedElement.getLookupElementBuilder(
    completionCtx: MvCompletionContext,
    scopeName: String,
    subst: Substitution = emptySubstitution,
): LookupElementBuilder {
    val base =
        LookupElementBuilder.createWithSmartPointer(scopeName, this)
            .withIcon(this.getIcon(0))
    val msl = completionCtx.msl
    return when (this) {
        is MvFunction -> {
            val functionTy = this.functionTy(msl).substitute(subst) as TyFunction
            if (completionCtx.contextElement is MvMethodOrField) {
                base
                    .withTailText(functionTy.parametersSignatureText())
                    .withTypeText(functionTy.returnTypeLookupText())
            } else {
                base
                    .withTailText(functionTy.signatureText())
                    .withTypeText(this.outerFileName)
            }
        }
        is MvSpecFunction -> {
            val functionTy = this.functionTy(msl).substitute(subst) as TyFunction
            base
                .withTailText(functionTy.parametersSignatureText())
                .withTypeText(functionTy.returnTypeLookupText())
        }

        is MvModule -> base
            .withTailText(this.addressRef()?.let { " ${it.text}" } ?: "")
            .withTypeText(this.containingFile?.name)

        is MvStruct -> {
            val tailText = this.tupleFields?.let {
                it.tupleFieldDeclList
                    .joinToString(", ", "(", ")") { it.type.text }
            }
            base.withTailText(tailText)
                .withTypeText(this.containingFile?.name)
        }

        is MvFieldDecl -> {
            val fieldTy = this.type?.loweredType(msl)?.substitute(subst) ?: TyUnknown
            base
                .withTypeText(fieldTy.text(false))
        }
        is MvConst -> {
            val constTy = this.type?.loweredType(msl) ?: TyUnknown
            base
                .withTypeText(constTy.text(true))
        }

        is MvPatBinding -> {
            val bindingInference = this.inference(msl)
            // race condition sometimes happens, when file is too big, inference is not finished yet
            val ty = bindingInference?.getPatTypeOrUnknown(this) ?: TyUnknown
            base
                .withTypeText(ty.text(true))
        }

        is MvSchema -> base
            .withTypeText(this.containingFile?.name)

        else -> base
    }
}
