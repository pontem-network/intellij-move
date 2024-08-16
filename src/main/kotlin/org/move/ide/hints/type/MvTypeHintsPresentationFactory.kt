package org.move.ide.hints.type

import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import org.move.ide.presentation.hintText
import org.move.lang.core.types.ty.*

@Suppress("UnstableApiUsage")
class MvTypeHintsPresentationFactory(private val factory: PresentationFactory) {

    fun typeHint(type: Ty): InlayPresentation = factory.roundWithBackground(
        listOf(text(": "), hint(type, 1)).join()
    )

    private fun hint(ty: Ty, level: Int): InlayPresentation = when (ty) {
        is TyTuple -> tupleTypeHint(ty, level)
        is TyReference -> referenceTypeHint(ty, level)
        is TyTypeParameter -> typeParameterTypeHint(ty)
        else -> text(ty.hintText())
    }

    private fun tupleTypeHint(type: TyTuple, level: Int): InlayPresentation =
        factory.collapsible(
            prefix = text("("),
            collapsed = text(PLACEHOLDER),
            expanded = { parametersHint(type.types, level + 1) },
            suffix = text(")"),
            startWithPlaceholder = checkSize(level, type.types.size)
        )

    private fun referenceTypeHint(type: TyReference, level: Int): InlayPresentation =
        listOf(
            text("&" + if (type.mutability.isMut) "mut " else ""),
            hint(type.referenced, level) // level is not incremented intentionally
        ).join()

    private fun typeParameterTypeHint(type: TyTypeParameter): InlayPresentation {
        return text(type.origin.name)
    }

    private fun parametersHint(kinds: List<Ty>, level: Int): InlayPresentation =
        kinds.map { hint(it, level) }.join(", ")

    private fun checkSize(level: Int, elementsCount: Int): Boolean = level + elementsCount > FOLDING_THRESHOLD

    private fun List<InlayPresentation>.join(separator: String = ""): InlayPresentation {
        if (separator.isEmpty()) {
            return factory.seq(*toTypedArray())
        }
        val presentations = mutableListOf<InlayPresentation>()
        var first = true
        for (presentation in this) {
            if (!first) {
                presentations.add(text(separator))
            }
            presentations.add(presentation)
            first = false
        }
        return factory.seq(*presentations.toTypedArray())
    }

    private fun text(text: String?): InlayPresentation = factory.smallText(text ?: "?")

    companion object {
        private const val PLACEHOLDER: String = "…"
        private const val FOLDING_THRESHOLD: Int = 3
    }
}
