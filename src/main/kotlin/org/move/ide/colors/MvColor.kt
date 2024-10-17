package org.move.ide.colors

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

enum class MvColor(humanName: String, default: TextAttributesKey? = null) {
    VARIABLE("Variables//Default", Default.IDENTIFIER),
    FIELD("Variables//Field", Default.INSTANCE_FIELD),

    KEY_OBJECT("Variables//Object with 'key'", Default.IDENTIFIER),
    STORE_OBJECT("Variables//Object with 'store'", Default.IDENTIFIER),
    STORE_NO_DROP_OBJECT("Variables//Object with 'store' but without 'drop'", Default.IDENTIFIER),

    REF("Variables//Reference", Default.IDENTIFIER),
    REF_TO_KEY_OBJECT("Variables//Reference to object with 'key'", Default.IDENTIFIER),
    REF_TO_STORE_OBJECT("Variables//Reference to object with 'store'", Default.IDENTIFIER),
    REF_TO_STORE_NO_DROP_OBJECT("Variables//Reference to object with 'store' but without 'drop'", Default.IDENTIFIER),

    MUT_REF("Variables//Mutable reference", Default.IDENTIFIER),
    MUT_REF_TO_KEY_OBJECT("Variables//Mutable reference to object with 'key' ability", Default.IDENTIFIER),
    MUT_REF_TO_STORE_OBJECT("Variables//Mutable reference to object with 'store' ability", Default.IDENTIFIER),
    MUT_REF_TO_STORE_NO_DROP_OBJECT("Variables//Mutable reference to object with 'store' ability but without 'drop'", Default.IDENTIFIER),

    MODULE("Modules//Module definition", Default.IDENTIFIER),
    CONSTANT("Variables//Constant", Default.CONSTANT),

    FUNCTION("Functions//Function declaration", Default.FUNCTION_DECLARATION),
    FUNCTION_CALL("Functions//Function call", Default.FUNCTION_CALL),

    VIEW_FUNCTION("Functions//View function declaration", Default.FUNCTION_DECLARATION),
    VIEW_FUNCTION_CALL("Functions//View function call", Default.FUNCTION_CALL),

    ENTRY_FUNCTION("Functions//Entry function declaration", Default.FUNCTION_DECLARATION),
    ENTRY_FUNCTION_CALL("Functions//Entry function call", Default.FUNCTION_CALL),

    INLINE_FUNCTION("Functions//Inline function declaration", Default.FUNCTION_DECLARATION),
    INLINE_FUNCTION_CALL("Functions//Inline function call", Default.FUNCTION_CALL),

    BUILTIN_FUNCTION_CALL("Functions//Builtins", Default.FUNCTION_CALL),

    METHOD("Functions//Method", Default.INSTANCE_METHOD),
    METHOD_CALL("Functions//Method call", Default.FUNCTION_CALL),

    MACRO("Functions//Macro", Default.IDENTIFIER),
    ATTRIBUTE("Attribute", Default.METADATA),

    KEYWORD("Keywords//Keyword", Default.KEYWORD),
    SELF_PARAMETER("Keywords//Self Parameter", Default.KEYWORD),

    ABILITY("Types//Ability", Default.IDENTIFIER),
    PRIMITIVE_TYPE("Types//Primitive", Default.KEYWORD),
    BUILTIN_TYPE("Types//Builtins", Default.IDENTIFIER),
    STRUCT("Types//Struct", Default.CLASS_NAME),
    TYPE_PARAMETER("Types//Generic type parameters", Default.IDENTIFIER),

    VECTOR_LITERAL("Literals//Vector", Default.FUNCTION_CALL),
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
