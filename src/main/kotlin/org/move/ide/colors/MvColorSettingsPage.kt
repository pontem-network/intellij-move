package org.move.ide.colors

import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.util.io.StreamUtil
import org.move.ide.MvHighlighter
import org.move.ide.MvIcons
import javax.swing.Icon

class MvColorSettingsPage : ColorSettingsPage {
    private val ATTRS = MvColor.values().map { it.attributesDescriptor }.toTypedArray()
    private val ANNOTATOR_TAGS = MvColor.values().associateBy({ it.name }, { it.textAttributesKey })

    private val DEMO_TEXT by lazy {
        // TODO: The annotations in this file should be generatable, and would be more accurate for it.
        val stream = javaClass.classLoader.getResourceAsStream("colors/highlighterDemoText.move")
            ?: error("No such file")
        StreamUtil.convertSeparators(StreamUtil.readText(stream, "UTF-8"))
    }

    override fun getDisplayName() = "Move"
    override fun getIcon(): Icon = MvIcons.MOVE
    override fun getHighlighter(): SyntaxHighlighter = MvHighlighter()
    override fun getAdditionalHighlightingTagToDescriptorMap() = ANNOTATOR_TAGS
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = ATTRS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDemoText(): String = DEMO_TEXT

}
