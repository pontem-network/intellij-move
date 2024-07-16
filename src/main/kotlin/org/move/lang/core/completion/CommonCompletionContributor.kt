package org.move.lang.core.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.impl.CompletionSorterImpl
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.completion.providers.*
import org.move.lang.core.completion.sort.COMPLETION_WEIGHERS_GROUPED
import org.move.lang.core.psi.MvModule

class CommonCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PrimitiveTypesCompletionProvider)
//        extend(CompletionType.BASIC, NamesCompletionProvider)
//        extend(CompletionType.BASIC, FunctionsCompletionProvider)
//        extend(CompletionType.BASIC, SchemasCompletionProvider)
        extend(CompletionType.BASIC, SpecItemCompletionProvider)
        extend(CompletionType.BASIC, AddressesCompletionProvider)
        extend(CompletionType.BASIC, AddressInModuleDeclCompletionProvider)
//        extend(CompletionType.BASIC, TypesCompletionProvider)
//        extend(CompletionType.BASIC, ImportsCompletionProvider)
//        extend(CompletionType.BASIC, ModulesCompletionProvider)
//        extend(CompletionType.BASIC, FQModuleCompletionProvider)
        extend(CompletionType.BASIC, StructFieldsCompletionProvider)
        extend(CompletionType.BASIC, StructPatCompletionProvider)
        extend(CompletionType.BASIC, SchemaFieldsCompletionProvider)
        extend(CompletionType.BASIC, MvPathCompletionProvider2)
        extend(
            CompletionType.BASIC,
            MvPsiPatterns.ability(),
            AbilitiesCompletionProvider
        )
        extend(
            CompletionType.BASIC,
            MvPsiPatterns.refExpr(),
            BoolsCompletionProvider
        )
        extend(CompletionType.BASIC, MacrosCompletionProvider)
        extend(CompletionType.BASIC, VectorLiteralCompletionProvider)
        extend(CompletionType.BASIC, MethodOrFieldCompletionProvider)
    }

    fun extend(type: CompletionType?, provider: MvCompletionProvider) {
        extend(type, provider.elementPattern, provider)
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val offset = context.startOffset
        val element = context.file.findElementAt(offset) ?: return
        if (element.parent is MvModule) {
            context.dummyIdentifier = "DummyAddress::"
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
