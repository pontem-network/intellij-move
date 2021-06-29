package org.move.ide.live_templates

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType

class MoveTemplateContext : TemplateContextType("MOVE", "Move") {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        return templateActionContext.file.name.endsWith(".move")
    }
}
