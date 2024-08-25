package org.move.lang.core.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.impl.CompletionSorterImpl
import com.intellij.psi.util.elementType
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.MvElementTypes.MODULE_KW
import org.move.lang.core.MvPsiPattern
import org.move.lang.core.completion.providers.*
import org.move.lang.core.completion.sort.COMPLETION_WEIGHERS_GROUPED
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.nextNonWsSibling
import org.move.lang.core.psi.ext.prevNonWsSibling

class CommonCompletionContributor: CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PrimitiveTypesCompletionProvider)
        extend(CompletionType.BASIC, SpecItemCompletionProvider)

        // addresses
        extend(CompletionType.BASIC, NamedAddressInUseStmtCompletionProvider)
        extend(CompletionType.BASIC, NamedAddressAtValueExprCompletionProvider)
        extend(CompletionType.BASIC, AddressInModuleDeclCompletionProvider)

        extend(CompletionType.BASIC, StructFieldsCompletionProvider)
        extend(CompletionType.BASIC, StructPatCompletionProvider)
        extend(CompletionType.BASIC, SchemaFieldsCompletionProvider)
        extend(CompletionType.BASIC, MvPathCompletionProvider2)

        extend(CompletionType.BASIC, MvPsiPattern.ability(), AbilitiesCompletionProvider)
//        extend(CompletionType.BASIC, MvPsiPattern.refExpr(), BoolsCompletionProvider)

        extend(CompletionType.BASIC, BoolsCompletionProvider)
        extend(CompletionType.BASIC, MacrosCompletionProvider)
        extend(CompletionType.BASIC, VectorLiteralCompletionProvider)
        extend(CompletionType.BASIC, MethodOrFieldCompletionProvider)

//        extend(CompletionType.BASIC, CommonCompletionProvider)
    }

    fun extend(type: CompletionType?, provider: MvCompletionProvider) {
        extend(type, provider.elementPattern, provider)
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val offset = context.startOffset
        val identifier = context.file.findElementAt(offset) ?: return
        if (identifier.parent is MvModule) {
            // check whether the left non-whitespace sibling is `module` keyword
            if (identifier.prevNonWsSibling.elementType == MODULE_KW) {
                context.dummyIdentifier = "DummyAddress::"
            }
        }
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        super.fillCompletionVariants(parameters, withSorter(parameters, result))
    }

    companion object {
        fun withSorter(
            parameters: CompletionParameters,
            result: CompletionResultSet
        ): CompletionResultSet {
            var sorter =
                (CompletionSorter.defaultSorter(parameters, result.prefixMatcher) as CompletionSorterImpl)
                    .withoutClassifiers { it.id == "liftShorter" }
            for (weigherGroup in COMPLETION_WEIGHERS_GROUPED) {
                sorter = sorter.weighAfter(weigherGroup.anchor, *weigherGroup.weighers)
            }
            return result.withRelevanceSorter(sorter)
        }
    }
}
