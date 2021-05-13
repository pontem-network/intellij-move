package org.move.ide.colors

import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.util.io.StreamUtil
import org.move.ide.MoveHighlighter
import org.move.ide.MoveIcons
import javax.swing.Icon

class MoveColorSettingsPage : ColorSettingsPage {
    private val ATTRS = MoveColor.values().map { it.attributesDescriptor }.toTypedArray()
    private val ANNOTATOR_TAGS = MoveColor.values().associateBy({ it.name }, { it.textAttributesKey })

    private val DEMO_TEXT by lazy {
        // TODO: The annotations in this file should be generatable, and would be more accurate for it.
        val stream = javaClass.classLoader.getResourceAsStream("colors/highlighterDemoText.move")
            ?: error("No such file")
        StreamUtil.convertSeparators(StreamUtil.readText(stream, "UTF-8"))
    }

    override fun getDisplayName() = "Move"
    override fun getIcon(): Icon = MoveIcons.MOVE
    override fun getHighlighter(): SyntaxHighlighter = MoveHighlighter()
    override fun getAdditionalHighlightingTagToDescriptorMap() = ANNOTATOR_TAGS
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = ATTRS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDemoText(): String = DEMO_TEXT

}
