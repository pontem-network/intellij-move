package org.move.ide.docs

import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import io.ktor.util.escapeHTML
import org.move.ide.colors.MvColor

@Suppress("UnstableApiUsage")
object MvColorUtils {

    val asKeyword get() = loadKey(MvColor.KEYWORD)
    val asFunction get() = loadKey(MvColor.FUNCTION)
    val asStruct get() = loadKey(MvColor.STRUCT)
    val asField get() = loadKey(MvColor.FIELD)
    val asEnum get() = loadKey(MvColor.ENUM)
    val asEnumVariant get() = loadKey(MvColor.ENUM_VARIANT)
    val asConst get() = loadKey(MvColor.CONSTANT)
    val asTypeParam get() = loadKey(MvColor.TYPE_PARAMETER)
    val asAbility get() = loadKey(MvColor.ABILITY)

    val asPrimitiveType get() = loadKey(MvColor.PRIMITIVE_TYPE)
    val asBuiltinType get() = loadKey(MvColor.BUILTIN_TYPE)
    val asStructType get() = loadKey(MvColor.STRUCT)
    val asEnumType get() = loadKey(MvColor.ENUM)
    val asEnumVariantType get() = loadKey(MvColor.ENUM_VARIANT)

    val asBraces get() = loadKey(MvColor.BRACES)
    val asBrackets get() = loadKey(MvColor.BRACKETS)
    val asParens get() = loadKey(MvColor.PARENTHESES)
    val asComma get() = loadKey(MvColor.COMMA)
    val asSemicolon get() = loadKey(MvColor.SEMICOLON)

    fun StringBuilder.keyword(text: String) = colored(text, asKeyword)
    fun StringBuilder.comma() = colored(",", asComma)
    fun StringBuilder.semicolon() = colored(";", asSemicolon)

    fun StringBuilder.colored(text: String?, color: TextAttributes, noHtml: Boolean = false) {
        if (noHtml) {
            append(text)
            return
        }
        HtmlSyntaxInfoUtil.appendStyledSpan(
            this, color, text?.escapeHTML() ?: "",
            DocumentationSettings.getHighlightingSaturation(false)
        )
    }

    private fun loadKey(color: MvColor): TextAttributes = loadKey(color.textAttributesKey)

    private fun loadKey(key: TextAttributesKey): TextAttributes =
        EditorColorsManager.getInstance().globalScheme.getAttributes(key)!!
}