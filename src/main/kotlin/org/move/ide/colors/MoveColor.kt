package org.move.ide.colors

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor

enum class MoveColor(humanName: String, default: TextAttributesKey? = null) {
    VARIABLE("Variables//Default", DefaultLanguageHighlighterColors.IDENTIFIER),
    FIELD("Variables//Field", DefaultLanguageHighlighterColors.INSTANCE_FIELD),

    MODULE_DEF("Modules//Module definition", DefaultLanguageHighlighterColors.IDENTIFIER),
    CONSTANT_DEF("Variables//Constant", DefaultLanguageHighlighterColors.CONSTANT),
    CONSTANT("Variables//Constant", DefaultLanguageHighlighterColors.CONSTANT),

    FUNCTION_DEF("Functions//Function declaration", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION),
    FUNCTION_CALL("Functions//Function call", DefaultLanguageHighlighterColors.FUNCTION_CALL),
    BUILTIN_FUNCTION_CALL("Functions//Builtins", DefaultLanguageHighlighterColors.IDENTIFIER),

    KEYWORD("Keywords//Keyword", DefaultLanguageHighlighterColors.KEYWORD),

    ABILITY("Types//Ability", DefaultLanguageHighlighterColors.IDENTIFIER),
    PRIMITIVE_TYPE("Types//Primitive", DefaultLanguageHighlighterColors.KEYWORD),
    BUILTIN_TYPE("Types//Builtins", DefaultLanguageHighlighterColors.IDENTIFIER),
    TYPE_PARAMETER("Types//Generic type parameters", DefaultLanguageHighlighterColors.IDENTIFIER);

    val textAttributesKey = TextAttributesKey.createTextAttributesKey("org.move.$name", default)
    val attributesDescriptor = AttributesDescriptor(humanName, textAttributesKey)
    val testSeverity: HighlightSeverity = HighlightSeverity(name, HighlightSeverity.INFORMATION.myVal)

}
