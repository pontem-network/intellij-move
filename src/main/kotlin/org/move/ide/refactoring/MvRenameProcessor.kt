package org.move.ide.refactoring

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.equalsTo
import org.move.lang.core.psi.ext.isShorthand
import org.move.lang.core.psi.ext.bindingOwner


class MvRenameProcessor: RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean =
        element is MvNamedElement && element !is MvTupleFieldDecl

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<out UsageInfo>,
        listener: RefactoringElementListener?
    ) {
        val psiFactory = element.project.psiFactory
        when (element) {
            is MvModule -> {
                // if only module in file and file has the same name -> rename file
                val file = element.containingMoveFile
                if (file != null) {
                    val filename = FileUtil.getNameWithoutExtension(file.name)
                    if (filename == element.name
                        && file.modules().singleOrNull()?.equalsTo(element) == true
                    ) {
                        file.name = "$newName.move"
                    }
                }
            }
            is MvNamedFieldDecl -> {
                usages.forEach {
                    val refElement = it.element ?: return@forEach
                    when {
                        refElement is MvStructLitField && refElement.isShorthand -> {
                            // NEW_FIELD_NAME: OLD_VARIABLE_NAME
                            // { myval } -> { newName: myval }
                            val newLitField = psiFactory.structLitField(newName, refElement.referenceName)
                            refElement.replace(newLitField)
                        }
                    }
                }
            }
            is MvPatBinding -> {
                val owner = element.bindingOwner
                usages.forEach {
                    when (owner) {
                        is MvSchemaFieldStmt -> {
                            // NEW_SCHEMA_FIELD_NAME: OLD_VARIABLE_NAME
                            val schemaLitField = it.element as? MvSchemaLitField ?: return@forEach
                            val newSchemaLitField =
                                psiFactory.schemaLitField(newName, schemaLitField.referenceName)
                            schemaLitField.replace(newSchemaLitField)
                        }
                        else -> {
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
                    }
                }
            }
        }

        val elementParent = element.parent
        val elementToRename = when {
            element is MvPatBinding && elementParent is MvPatField -> {
                // replace { myval } -> { myval: myval }
                val newFieldPat = psiFactory.fieldPatFull(element.referenceName, element.referenceName)
                val newFieldPatInTree = elementParent.patBinding?.replace(newFieldPat) as MvPatFieldFull
                newFieldPatInTree.pat as MvPatBinding
            }
            else -> element
        }
        super.renameElement(elementToRename, newName, usages, listener)
    }

    private val PsiElement.maybeLitFieldParent
        get() = PsiTreeUtil
            .findFirstParent(this) { it is MvStructLitField || it is MvSchemaLitField }
}
