package org.move.toml

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.util.text.findTextRange
import org.move.ide.annotator.MvAnnotatorBase
import org.move.openapiext.stringValue
import org.toml.lang.psi.TomlTable

class MoveTomlErrorAnnotator : MvAnnotatorBase() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile ?: return
        if (file.name != "Move.toml") {
            return
        }
        if (element !is TomlTable) return

        val tableKey = element.header.key ?: return
        if (!tableKey.textMatches("addresses")) return

        for (tomlKeyValue in element.entries) {
            val tomlValue = tomlKeyValue.value ?: continue
            val rawStringValue = tomlValue.stringValue() ?: continue
            if (rawStringValue == "_") continue
            val tomlString = rawStringValue.removePrefix("0x")
            val stringRange =
                tomlValue.text.findTextRange(tomlString)?.shiftRight(tomlValue.textOffset)
                    ?: tomlValue.textRange
            if (tomlString.length > 64) {
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "Invalid address: no more than 64 symbols allowed"
                )
                    .range(stringRange)
                    .create()
                return
            }
            if (DIEM_ADDRESS_REGEX.matchEntire(tomlString) == null) {
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "Invalid address: only hex symbols are allowed"
                )
                    .range(stringRange)
                    .create()
                return
            }
        }
    }

    companion object {
        private val DIEM_ADDRESS_REGEX = Regex("[0-9a-fA-F]{1,64}")
    }
}