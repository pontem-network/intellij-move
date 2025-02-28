package org.move.ide.docs

import com.intellij.codeEditor.printing.HTMLTextPainter
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.psi.PsiElement
import com.intellij.ui.ColorHexUtil
import com.intellij.ui.ColorUtil
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.SimpleTagProvider
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser
import java.net.URI

enum class MvDocRenderMode {
    /**
     * Hover Documentation mode.
     */
    QUICK_DOC_POPUP,

    /**
     * Inline Editor Documentation mode.
     *
     * Such documentation is displayed directly in the editor.
     */
    INLINE_DOC_COMMENT,
}

fun documentationAsHtml(text: String, context: PsiElement): String {
    val documentationText = processDocumentationText(text)
    val flavour = MvDocMarkdownFlavourDescriptor(context, null, MvDocRenderMode.QUICK_DOC_POPUP)
    val root = MarkdownParser(flavour).buildMarkdownTreeFromString(documentationText)
    return HtmlGenerator(documentationText, root, flavour).generateHtml()
}

/**
 * Prepares passed a comment text for the Markdown parser.
 *
 * For example, a line with a dot at the end will be split by "\n\n" to separate two paragraphs,
 * without this, [HtmlGenerator] will render it as one paragraph for this and the next lines.
 */
fun processDocumentationText(text: String): String {
    // Comments spanning multiple lines are merged using spaces, unless
    //
    // - the line is empty
    // - the line ends with a . (end of sentence)
    // - the line is purely of at least 3 of -, =, _, *, ~ (horizontal rule)
    // - the line starts with at least one # followed by a space (header)
    // - the line starts and ends with a | (table)
    // - the line starts with - (list)

    var insideCodeBlock = false
    val lines = text.lines()
    val newLines = lines.map { l ->
        // don't trim extra spaces in code blocks
        val line = if (insideCodeBlock) l.trimStart('/').removePrefix(" ") else l.trimStart('/', ' ')
        if (line.startsWith("```")) {
            insideCodeBlock = !insideCodeBlock
        } else if (insideCodeBlock) {
            // don't add any extra new lines to code blocks
            return@map line
        }

        if (ORDERED_LIST_REGEX.matches(line)) {
            line // don't add any extra new lines to lists
        } else if (line.endsWith(".") || line.endsWith("!") || line.endsWith("?") ||
            line.matches(Regex("^[-=_*~]{3,}\$")) ||
            line.endsWith("|") ||
            line.startsWith("|") ||
            line.startsWith("-")
        ) {
            line + "\n\n"
        } else {
            line
        }
    }

    return newLines.joinToString("\n")
}

val ORDERED_LIST_REGEX = """^(\d+\.|-|\*)\s.*$""".toRegex()

/**
 * Defines how to render Markdown into HTML.
 */
private class MvDocMarkdownFlavourDescriptor(
    private val context: PsiElement,
    private val uri: URI? = null,
    private val renderMode: MvDocRenderMode,
    private val gfm: MarkdownFlavourDescriptor = GFMFlavourDescriptor(useSafeLinks = false, absolutizeAnchorLinks = true),
) : MarkdownFlavourDescriptor by gfm {

    override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?): Map<IElementType, GeneratingProvider> {
        val generatingProviders = HashMap(gfm.createHtmlGeneratingProviders(linkMap, uri ?: baseURI))
        // Filter out MARKDOWN_FILE to avoid producing unnecessary <body> tags
        generatingProviders.remove(MarkdownElementTypes.MARKDOWN_FILE)
        // Use smaller header providers as h1 and h2 are too large
        generatingProviders[MarkdownElementTypes.ATX_1] = SimpleTagProvider("h2")
        generatingProviders[MarkdownElementTypes.ATX_2] = SimpleTagProvider("h3")
        generatingProviders[MarkdownElementTypes.CODE_FENCE] = MvCodeFenceProvider(context, renderMode)

        return generatingProviders
    }
}

/**
 * Defines how to render multiline code blocks.
 */
private class MvCodeFenceProvider(
    private val context: PsiElement,
    private val renderMode: MvDocRenderMode,
) : GeneratingProvider {

    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        val indentBefore = node.getTextInNode(text).commonPrefixWith(" ".repeat(10)).length

        val codeText = StringBuilder()

        var childrenToConsider = node.children
        if (childrenToConsider.last().type == MarkdownTokenTypes.CODE_FENCE_END) {
            childrenToConsider = childrenToConsider.subList(0, childrenToConsider.size - 1)
        }

        var isContentStarted = false

        for (child in childrenToConsider) {
            if (isContentStarted && child.type in listOf(MarkdownTokenTypes.CODE_FENCE_CONTENT, MarkdownTokenTypes.EOL)) {
                val rawLine = HtmlGenerator.trimIndents(child.getTextInNode(text), indentBefore)
                codeText.append(rawLine)
            }

            if (!isContentStarted && child.type == MarkdownTokenTypes.EOL) {
                isContentStarted = true
            }
        }

        visitor.consumeHtml(convertToHtmlWithHighlighting(codeText.toString()))
    }

    private fun convertToHtmlWithHighlighting(codeText: String): String {
        var htmlCodeText = HTMLTextPainter.convertCodeFragmentToHTMLFragmentWithInlineStyles(context, codeText)

        val scheme = EditorColorsManager.getInstance().globalScheme

        // replace <pre> with <pre> with nicer looking styles in the rendered view
        htmlCodeText = htmlCodeText.replaceFirst(
            "<pre>",
            "<pre style=\"text-indent: ${CODE_SNIPPET_INDENT}px; margin-bottom: -20px;\">"
        )

        return when (renderMode) {
            MvDocRenderMode.INLINE_DOC_COMMENT -> htmlCodeText.dimColors(scheme)
            else                               -> htmlCodeText
        }
    }

    /**
     * Makes the colors muted to show it in the comment that is rendered directly in the code.
     * This function is mostly for minor visual improvements.
     *
     * TODO: this code is not used yet, but we will need it for the next steps
     */
    private fun String.dimColors(scheme: EditorColorsScheme): String {
        val alpha = if (isColorSchemeDark(scheme)) DARK_THEME_ALPHA else LIGHT_THEME_ALPHA

        return replace(COLOR_PATTERN) { result ->
            val colorHexValue = result.groupValues[1]
            val fgColor = ColorHexUtil.fromHexOrNull(colorHexValue) ?: return@replace result.value
            val bgColor = scheme.defaultBackground
            val finalColor = ColorUtil.mix(bgColor, fgColor, alpha)

            "color: #${ColorUtil.toHex(finalColor)}"
        }
    }

    private fun isColorSchemeDark(scheme: EditorColorsScheme): Boolean {
        return ColorUtil.isDark(scheme.defaultBackground)
    }

    companion object {
        private val COLOR_PATTERN = """color:\s*#(\p{XDigit}{3,})""".toRegex()

        private const val CODE_SNIPPET_INDENT = 10
        private const val LIGHT_THEME_ALPHA = 0.6
        private const val DARK_THEME_ALPHA = 0.78
    }
}
