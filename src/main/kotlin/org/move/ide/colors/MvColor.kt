package org.move.ide.colors

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor

enum class MvColor(humanName: String, default: TextAttributesKey? = null) {
    VARIABLE("Variables//Default", Default.IDENTIFIER),
    FIELD("Variables//Field", Default.INSTANCE_FIELD),

    MODULE_DEF("Modules//Module definition", Default.IDENTIFIER),
    CONSTANT_DEF("Variables//Constant", Default.CONSTANT),
    CONSTANT("Variables//Constant", Default.CONSTANT),

    FUNCTION_DEF("Functions//Function declaration", Default.FUNCTION_DECLARATION),
    FUNCTION_CALL("Functions//Function call", Default.FUNCTION_CALL),
    BUILTIN_FUNCTION_CALL("Functions//Builtins", Default.IDENTIFIER),

    KEYWORD("Keywords//Keyword", Default.KEYWORD),

    ABILITY("Types//Ability", Default.IDENTIFIER),
    PRIMITIVE_TYPE("Types//Primitive", Default.KEYWORD),
    BUILTIN_TYPE("Types//Builtins", Default.IDENTIFIER),
    TYPE_PARAMETER("Types//Generic type parameters", Default.IDENTIFIER),

    NUMBER("Literals//Number", Default.NUMBER),
    ADDRESS("Literals//Address", Default.NUMBER),
    STRING("Literals//String", Default.STRING),

    BLOCK_COMMENT("Comments//Block comment", Default.BLOCK_COMMENT),
    EOL_COMMENT("Comments//Line comment", Default.LINE_COMMENT),

    DOC_COMMENT("Docs//Comment", Default.DOC_COMMENT),

    BRACES("Braces and Operators//Braces", Default.BRACES),
    BRACKETS("Braces and Operators//Brackets", Default.BRACKETS),
    OPERATORS("Braces and Operators//Operation sign", Default.OPERATION_SIGN),
    SEMICOLON("Braces and Operators//Semicolon", Default.SEMICOLON),
    DOT("Braces and Operators//Dot", Default.DOT),
    COMMA("Braces and Operators//Comma", Default.COMMA),
    PARENTHESES("Braces and Operators//Parentheses", Default.PARENTHESES),
    ;

    val textAttributesKey = TextAttributesKey.createTextAttributesKey("org.move.$name", default)
    val attributesDescriptor = AttributesDescriptor(humanName, textAttributesKey)
    val testSeverity: HighlightSeverity = HighlightSeverity(name, HighlightSeverity.INFORMATION.myVal)

}
