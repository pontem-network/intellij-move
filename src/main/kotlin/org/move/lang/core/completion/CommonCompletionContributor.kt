package org.move.lang.core.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.impl.CompletionSorterImpl
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementWeigher
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.completion.providers.*
import org.move.lang.core.completion.sort.COMPLETION_WEIGHERS
import org.move.lang.core.completion.sort.MvCompletionWeigher
import org.move.lang.core.psi.MvModule

class CommonCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PrimitiveTypesCompletionProvider)
        extend(CompletionType.BASIC, NamesCompletionProvider)
        extend(CompletionType.BASIC, SchemasCompletionProvider)
        extend(CompletionType.BASIC, SpecItemCompletionProvider)
        extend(CompletionType.BASIC, AddressesCompletionProvider)
        extend(CompletionType.BASIC, AddressInModuleDeclCompletionProvider)
        extend(CompletionType.BASIC, TypesCompletionProvider)
        extend(CompletionType.BASIC, ImportsCompletionProvider)
        extend(CompletionType.BASIC, ModulesCompletionProvider)
        extend(CompletionType.BASIC, FQModuleCompletionProvider)
        extend(CompletionType.BASIC, StructFieldsCompletionProvider)
        extend(CompletionType.BASIC, StructPatCompletionProvider)
        extend(CompletionType.BASIC, SchemaFieldsCompletionProvider)
        extend(
            CompletionType.BASIC,
            MvPsiPatterns.ability(),
            TraitsCompletionProvider
        )
        extend(CompletionType.BASIC, MacrosCompletionProvider)
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
        private val RS_COMPLETION_WEIGHERS_GROUPED: List<AnchoredWeigherGroup> =
            splitIntoGroups(COMPLETION_WEIGHERS)

        fun withSorter(
            parameters: CompletionParameters,
            result: CompletionResultSet
        ): CompletionResultSet {
            var sorter =
                (CompletionSorter.defaultSorter(parameters, result.prefixMatcher) as CompletionSorterImpl)
                    .withoutClassifiers { it.id == "liftShorter" }
            for (weigherGroups in RS_COMPLETION_WEIGHERS_GROUPED) {
                sorter = sorter.weighAfter(weigherGroups.anchor, *weigherGroups.weighers)
            }
            return result.withRelevanceSorter(sorter)
        }

        private fun splitIntoGroups(weighersWithAnchors: List<Any>): List<AnchoredWeigherGroup> {
            val firstEntry = weighersWithAnchors.firstOrNull() ?: return emptyList()
            check(firstEntry is String) {
                "The first element in the weigher list must be a string placeholder like \"priority\"; " +
                        "actually it is `${firstEntry}`"
            }
            val result = mutableListOf<AnchoredWeigherGroup>()
            val weigherIds = hashSetOf<String>()
            var currentAnchor: String = firstEntry
            var currentWeighers = mutableListOf<LookupElementWeigher>()
            // Add "dummy weigher" in order to execute `is String ->` arm in the last iteration
            for (weigherOrAnchor in weighersWithAnchors.asSequence().drop(1)
                .plus(sequenceOf("dummy weigher"))) {
                when (weigherOrAnchor) {
                    is String -> {
                        if (currentWeighers.isNotEmpty()) {
                            result += AnchoredWeigherGroup(currentAnchor, currentWeighers.toTypedArray())
                            currentWeighers = mutableListOf()
                        }
                        currentAnchor = weigherOrAnchor
                    }
                    is MvCompletionWeigher -> {
                        if (!weigherIds.add(weigherOrAnchor.id)) {
                            error(
                                "Found a ${MvCompletionWeigher::class.simpleName}.id duplicate: " +
                                        "`${weigherOrAnchor.id}`"
                            )
                        }
                        currentWeighers += RsCompletionWeigherAsLookupElementWeigher(weigherOrAnchor)
                    }
                    else -> error(
                        "The weigher list must consists of String placeholders and instances of " +
                                "${MvCompletionWeigher::class.simpleName}, got ${weigherOrAnchor.javaClass}"
                    )
                }
            }
            return result
        }

        private class AnchoredWeigherGroup(val anchor: String, val weighers: Array<LookupElementWeigher>)

        private class RsCompletionWeigherAsLookupElementWeigher(
            private val weigher: MvCompletionWeigher
        ) : LookupElementWeigher(weigher.id, /* negated = */ false, /* dependsOnPrefix = */ false) {
            override fun weigh(element: LookupElement): Comparable<*> {
                val rsElement = element.`as`(LookupElement::class.java)
                return weigher.weigh(rsElement ?: element)
            }
        }
    }
}
