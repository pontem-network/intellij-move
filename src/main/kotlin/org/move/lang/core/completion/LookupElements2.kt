package org.move.lang.core.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.move.ide.presentation.text
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.TyUnknown

fun createCompletionItem(
    scopeEntry: ScopeEntry,
    completionContext: MvCompletionContext,
    subst: Substitution = emptySubstitution,
    priority: Double = DEFAULT_PRIORITY,
    insertHandler: InsertHandler<LookupElement> = DefaultInsertHandler(completionContext)
): CompletionItem {
    val element = scopeEntry.element()
    val lookup = element
        .getLookupElementBuilder(completionContext, scopeEntry.name, subst)
        .withInsertHandler(insertHandler)
        .withPriority(priority)
    val props = getLookupElementProperties(element, subst, completionContext)
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
            val signature = FuncSignature.fromFunction(this, msl).substitute(subst)
            if (completionCtx.contextElement is MvMethodOrField) {
                base
                    .withTailText(signature.paramsText())
                    .withTypeText(signature.retTypeText())
            } else {
                base
                    .withTailText(this.signatureText)
                    .withTypeText(this.outerFileName)
            }
        }
        is MvSpecFunction -> base
            .withTailText(this.parameters.joinToSignature())
            .withTypeText(this.returnType?.type?.text ?: "()")

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

