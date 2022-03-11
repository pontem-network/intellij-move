package org.move.ide.refactoring

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.descendantOfTypeStrict
import org.move.lang.core.psi.ext.isShorthand

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
            is MvBindingPat -> {
                usages.forEach {
                    val field = PsiTreeUtil.findFirstParent(it.element) {
                        it is MvStructLitField || it is MvSchemaLitField
                    }
                    when (field) {
                        is MvStructLitField -> {
                            if (field.isShorthand) {
                                val newField = psiFactory.createStructLitField(field.referenceName, newName)
                                field.replace(newField)
                            }
                        }
                        is MvSchemaLitField -> {
                            if (field.isShorthand) {
                                val newField = psiFactory.createSchemaLitField(field.referenceName, newName)
                                field.replace(newField)
                            }
                        }
                    }
//                    val field =
//                        it.element?.ancestorOrSelf<MvStructLitField>(MvStructLitExpr::class.java)
//                            ?: return@forEach
//                    if (field.isShorthand) {
//                        val newField = psiFactory.createStructLitField(field.referenceName, newName)
//                        field.replace(newField)
//                    }
                }
            }
//            is SchemaFieldStmt -> {
//                usages.forEach {
//                    val field =
//                        it.element?.ancestorOrSelf<MvSchemaField>(MvSchemaLit::class.java) ?: return@forEach
//                    if (field.isShorthand) {
//                        val newField = psiFactory.createSchemaLitField(newName, field.referenceName)
//                        field.replace(newField)
//                    }
//                }
//            }
        }

        val elementToRename = when {
            element is MvBindingPat && element.parent is MvStructPatField -> {
                val newPatField = psiFactory.createStructPatField(element.identifier.text, element.text)
                element.replace(newPatField).descendantOfTypeStrict<MvBindingPat>()!!
            }
//            element is MvBindingPat && element.parent is MvSchemaField -> {
//
//            }
            else -> element
        }
//        if (element is MvBindingPat) {
//
//        }
//        when {
//            element is MvBindingPat
//        }

//        val newRenameElement = if (element is MvBindingPat && element.parent is MvStructPatField) {
//            val newPatField = psiFactory.createStructPatField(element.identifier.text, element.text)
//            element.replace(newPatField).descendantOfTypeStrict<MvBindingPat>()!!
//        } else element

        super.renameElement(elementToRename, newName, usages, listener)
    }
}
