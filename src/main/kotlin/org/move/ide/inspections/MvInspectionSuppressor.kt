package org.move.ide.inspections

import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import org.move.lang.core.MV_COMMENTS
import org.move.lang.core.psi.MvAttr
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.psiFactory
import javax.swing.Icon

class MvInspectionSuppressor: InspectionSuppressor {
    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<out SuppressQuickFix> {
        val ancestors = element?.ancestors.orEmpty().filterIsInstance<MvDocAndAttributeOwner>()
        // todo: add suppression with comments for other inspections
        if (toolId !in APTOS_LINTS) return emptyArray()
        return ancestors.mapNotNull {
            when (it) {
                is MvFunction -> {
                    SuppressInspectionWithAttributeFix(it, toolId, "function")
                }
                is MvModule -> SuppressInspectionWithAttributeFix(it, toolId, "module")
                else -> null
            }
        }.toList().toTypedArray()
    }

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        return element.ancestors.filterIsInstance<MvDocAndAttributeOwner>()
            .any {
                isSuppressedByComment(it, toolId)
                        || isSuppressedByAttribute(it, toolId)
            }
    }

    private fun isSuppressedByComment(element: MvDocAndAttributeOwner, toolId: String): Boolean {
        return element.leadingComments().any { comment ->
            val matcher = SuppressionUtil.SUPPRESS_IN_LINE_COMMENT_PATTERN.matcher(comment.text)
            matcher.matches() && SuppressionUtil.isInspectionToolIdMentioned(matcher.group(1), toolId)
        }
    }

    // special alternativeId for some inspection that could be suppressed by the attributes, in form of
    // lint::LINT_NAME, and it's suppressable by the #[lint::skip(LINT_NAME)]
    private fun isSuppressedByAttribute(element: MvDocAndAttributeOwner, toolId: String): Boolean {
        if (toolId !in APTOS_LINTS) return false
        return element.queryAttributes
            .getAttrItemsByPath("lint::skip")
            .any { it.innerAttrItems.any { it.textMatches(toolId) } }
    }

    @Suppress("PrivatePropertyName")
    private val APTOS_LINTS = setOf(MvNeedlessDerefRefInspection.LINT_ID)
}

private class SuppressInspectionWithAttributeFix(
    item: MvDocAndAttributeOwner,
    val toolId: String,
    val itemId: String,
): LocalQuickFixOnPsiElement(item), ContainerBasedSuppressQuickFix, Iconable {

    override fun getFamilyName(): @IntentionFamilyName String = "Suppress '$toolId' inspections"
    override fun getText(): @IntentionName String = "Suppress with '#[lint::skip($toolId)]' for $itemId"

    override fun isSuppressAll(): Boolean = false
    override fun getIcon(flags: Int): Icon? = AllIcons.Ide.HectorOff

    override fun getContainer(context: PsiElement?): PsiElement? = this.startElement

    override fun isAvailable(
        project: Project,
        context: PsiElement
    ): Boolean = context.isValid && getContainer(context) != null

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val element = startElement as? MvDocAndAttributeOwner ?: return
        val skipAttribute = project.psiFactory.attribute("#[lint::skip($toolId)]")
        val anchor = element.childrenWithLeaves
            .dropWhile { it is PsiComment || it is PsiWhiteSpace || it is MvAttr }.firstOrNull()
        val addedAttribute = element.addBefore(skipAttribute, anchor)
        element.addAfter(project.psiFactory.newline(), addedAttribute)
    }
}

private fun MvDocAndAttributeOwner.leadingComments(): Sequence<PsiComment> =
    generateSequence(firstChild) { psi ->
        psi.nextSibling.takeIf { it.elementType in MV_COMMENTS || it is PsiWhiteSpace }
    }
        .filterIsInstance<PsiComment>()
