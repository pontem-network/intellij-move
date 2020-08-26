package org.move.ide.colors

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor

enum class MvColor(humanName: String, default: TextAttributesKey? = null) {
    IDENTIFIER("Variables//Default", DefaultLanguageHighlighterColors.IDENTIFIER),
    KEYWORD("Keywords//Keyword", DefaultLanguageHighlighterColors.KEYWORD),
    PRIMITIVE_TYPE("Types//Primitive", DefaultLanguageHighlighterColors.KEYWORD);

    val textAttributesKey = TextAttributesKey.createTextAttributesKey("org.move.$name", default)
    val attributesDescriptor = AttributesDescriptor(humanName, textAttributesKey)
    val testSeverity: HighlightSeverity = HighlightSeverity(name, HighlightSeverity.INFORMATION.myVal)

}