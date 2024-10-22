package org.move.ide

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import org.move.lang.MoveLanguage
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.expr

class MvBreadcrumbsProvider : BreadcrumbsProvider {

    private interface MvElementHandler<T : MvElement> {
        fun accepts(e: PsiElement): Boolean
        fun elementInfo(e: T): String
    }

    private val handlers = listOf<MvElementHandler<*>>(
        MvModuleHandler,
        MvModuleSpecHandler,
        MvItemSpecHandler,
        MvFunctionHandler,
        MvIfHandler,
        MvElseHandler,
        MvWhileHandler,
        MvBlockHandler,
        MvNamedHandler,
        MvForHandler,
    )

    private object MvNamedHandler : MvElementHandler<MvNamedElement> {
        override fun accepts(e: PsiElement): Boolean = e is MvNamedElement

        override fun elementInfo(e: MvNamedElement): String = e.name.let { "$it" }
    }

    private object MvModuleHandler : MvElementHandler<MvModule> {
        override fun accepts(e: PsiElement): Boolean = e is MvModule

        override fun elementInfo(e: MvModule): String = e.qualName?.editorText() ?: "null"
    }

    private object MvModuleSpecHandler : MvElementHandler<MvModuleSpec> {
        override fun accepts(e: PsiElement): Boolean = e is MvModuleSpec

        override fun elementInfo(e: MvModuleSpec): String {
            return buildString {
                append("spec:")
                append(" ").append(e.path?.text ?: "null")
            }
        }
    }

    private object MvItemSpecHandler : MvElementHandler<MvItemSpec> {
        override fun accepts(e: PsiElement): Boolean = e is MvItemSpec

        override fun elementInfo(e: MvItemSpec): String {
            return buildString {
                append("spec:")
                append(" ").append(e.itemSpecRef?.text ?: "null")
            }
        }
    }

    private object MvFunctionHandler : MvElementHandler<MvFunction> {
        override fun accepts(e: PsiElement): Boolean = e is MvFunction

        override fun elementInfo(e: MvFunction): String = e.name.let { "$it()" }
    }

    private object MvBlockHandler : MvElementHandler<MvCodeBlock> {
        override fun accepts(e: PsiElement): Boolean = e is MvCodeBlock

        override fun elementInfo(e: MvCodeBlock): String = "{...}"
    }

    private object MvIfHandler : MvElementHandler<MvIfExpr> {
        override fun accepts(e: PsiElement): Boolean = e is MvIfExpr

        override fun elementInfo(e: MvIfExpr): String {
            return buildString {
                append("if")
                val condition = e.condition
                if (condition != null) {
                    if (condition.expr is MvCodeBlock) {
                        append(" {...}")
                    } else {
                        append(' ').append(condition.text.truncate(TextKind.INFO))
                    }
                }
            }
        }
    }

    private object MvElseHandler : MvElementHandler<MvElseBlock> {
        override fun accepts(e: PsiElement): Boolean = e is MvElseBlock

        override fun elementInfo(e: MvElseBlock): String = "else"
    }

    private object MvWhileHandler : MvElementHandler<MvWhileExpr> {
        override fun accepts(e: PsiElement): Boolean = e is MvWhileExpr

        override fun elementInfo(e: MvWhileExpr): String {
            return buildString {
                append("while")
                val condition = e.condition
                if (condition != null) {
                    if (condition.expr is MvCodeBlock) {
                        append(" {...}")
                    } else {
                        append(' ').append(condition.text.truncate(TextKind.INFO))
                    }
                }
            }
        }
    }

    private object MvForHandler : MvElementHandler<MvForExpr> {
        override fun accepts(e: PsiElement): Boolean = e is MvForExpr

        override fun elementInfo(e: MvForExpr): String {
            return buildString {
                append("for")
                val condition = e.forIterCondition
                if (condition != null) {
                    append('(')
                    val binding = condition.patBinding
                    if (binding != null) {
                        append(binding.text)
                    }
                    append(" in ").append(condition.expr?.text?.truncate(TextKind.INFO))
                    append(')')
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handler(e: PsiElement): MvElementHandler<in MvElement>? {
        return if (e is MvElement)
            handlers.firstOrNull { it.accepts(e) } as MvElementHandler<in MvElement>?
        else null
    }

    override fun getLanguages(): Array<Language> = LANGUAGES

    override fun acceptElement(e: PsiElement): Boolean = handler(e) != null

    override fun getElementInfo(e: PsiElement): String = handler(e)!!.elementInfo(e as MvElement)

    override fun getElementTooltip(e: PsiElement): String? = null

    companion object {
        @Suppress("unused")
        private enum class TextKind(val maxTextLength: Int) {
            INFO(16),
            TOOLTIP(100)
        }

        private val LANGUAGES: Array<Language> = arrayOf(MoveLanguage)

        const val ellipsis = "${Typography.ellipsis}"

        private fun String.truncate(kind: TextKind): String {
            val maxLength = kind.maxTextLength
            return if (length > maxLength)
                "${substring(0, maxLength - ellipsis.length)}$ellipsis"
            else this
        }
    }
}
