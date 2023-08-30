package org.move.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvAddressDef
import org.move.lang.core.psi.MvModuleBlock
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.hasAncestorOrSelf
import org.move.lang.core.psi.ext.modules
import org.move.lang.core.psi.psiFactory

class InlineAddressBlockIntention : MvElementBaseIntentionAction<InlineAddressBlockIntention.Context>() {

    override fun getText(): String = "Inline address block"

    override fun getFamilyName(): String = text

    data class Context(val address: MvAddressDef)

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        if (element.hasAncestorOrSelf<MvModuleBlock>()) return null
        val address = element.ancestorStrict<MvAddressDef>() ?: return null
        if (address.modules().size != 1) return null
        return Context(address)
    }

    override fun invoke(project: Project, editor: Editor, ctx: Context) {
        val address = ctx.address
        val addressText = address.addressRef?.text ?: return
        val module = address.modules().firstOrNull() ?: return
        val moduleNameElement = module.nameElement ?: return
        val blockText = module.moduleBlock?.text ?: return

        val inlineModule = project.psiFactory.inlineModule(addressText, moduleNameElement.text, blockText)
        address.replace(inlineModule)
    }
}
