package org.move.ide.refactoring

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.descendantOfTypeStrict
import org.move.lang.core.psi.ext.isShorthand
import org.move.lang.core.psi.ext.owner

val PsiElement.maybeLitFieldParent
    get() = PsiTreeUtil.findFirstParent(this) {
        it is MvStructLitField || it is MvSchemaLitField
    }


class MvRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement) = element is MvNamedElement

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<out UsageInfo>,
        listener: RefactoringElementListener?
    ) {
        val psiFactory = element.project.psiFactory
        when (element) {
            is MvStructField -> {
                usages.forEach {
                    val usage = it.element
                    when {
                        usage is MvStructLitField && usage.isShorthand -> {
                            // NEW_FIELD_NAME: OLD_VARIABLE_NAME
                            val newLitField = psiFactory.structLitField(newName, usage.referenceName)
                            usage.replace(newLitField)
                        }
                        usage is MvStructPatField && usage.isShorthand -> {
                            // NEW_PAT_FIELD_NAME: OLD_VARIABLE_NAME
                            val newPatField = psiFactory.structPatField(newName, usage.referenceName)
                            usage.replace(newPatField)
                        }
                    }
                }
            }
            is MvBindingPat -> {
                val owner = element.owner
                usages.forEach {
                    when (owner) {
                        is MvLetStmt -> {
                            val field = it.element?.maybeLitFieldParent
                            // OLD_FIELD_NAME: NEW_VARIABLE_NAME
                            when {
                                field is MvStructLitField && field.isShorthand -> {
                                    val newField =
                                        psiFactory.structLitField(field.referenceName, newName)
                                    field.replace(newField)
                                }
                                field is MvSchemaLitField && field.isShorthand -> {
                                    val newField =
                                        psiFactory.schemaLitField(field.referenceName, newName)
                                    field.replace(newField)
                                }
                            }
                        }
                        is MvSchemaFieldStmt -> {
                            // NEW_SCHEMA_FIELD_NAME: OLD_VARIABLE_NAME
                            val schemaLitField = it.element as? MvSchemaLitField ?: return@forEach
                            val newSchemaLitField =
                                psiFactory.schemaLitField(newName, schemaLitField.referenceName)
                            schemaLitField.replace(newSchemaLitField)
                        }
                    }
                }
            }
        }

        val elementToRename = when {
            element is MvBindingPat && element.parent is MvStructPatField -> {
                val newPatField = psiFactory.structPatField(element.identifier.text, element.text)
                element.replace(newPatField).descendantOfTypeStrict<MvBindingPat>()!!
            }
            else -> element
        }
        super.renameElement(elementToRename, newName, usages, listener)
    }
}
