package org.move.ide.colors

import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.util.io.StreamUtil
import org.move.ide.MoveIcons
import org.move.ide.MvHighlighter

class MvColorSettingsPage : ColorSettingsPage {
    private val DEMO_TEXT by lazy {
        // TODO: The annotations in this file should be generatable, and would be more accurate for it.
        val stream = javaClass.classLoader.getResourceAsStream("colors/highlighterDemoText.move")
            ?: error("No such file")
        val text = stream.bufferedReader().use { it.readText() }
        StreamUtil.convertSeparators(text)
    }

    override fun getDisplayName() = "Move"
    override fun getIcon() = MoveIcons.MOVE_LOGO
    override fun getHighlighter() = MvHighlighter()
    override fun getAdditionalHighlightingTagToDescriptorMap() = ANNOTATOR_TAGS
    override fun getAttributeDescriptors() = ATTRS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDemoText() = DEMO_TEXT

    companion object {
        private val ATTRS = MvColor.values().map { it.attributesDescriptor }.toTypedArray()
        private val ANNOTATOR_TAGS = MvColor.values().associateBy({ it.name }, { it.textAttributesKey })
    }
}
