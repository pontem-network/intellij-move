package org.move.lang.core.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.move.lang.MoveFile
import org.move.lang.MoveFileType
import org.move.lang.core.psi.ext.descendantOfTypeStrict

class MovePsiFactory(private val project: Project) {
    fun createIdentifier(text: String): PsiElement =
        createFromText<MoveModuleDef>("module $text {}")?.nameIdentifier
            ?: error("Failed to create identifier: `$text`")

    fun createNativeFunctionDef(
        text: String,
    ): MoveNativeFunctionDef =
        createFromText("module _IntellijPreludeDummy { $text }")
            ?: error("Failed to create a method member from text: `$text`")

    private inline fun <reified T : MoveElement> createFromText(code: CharSequence): T? {
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText(
                "DUMMY.move",
                MoveFileType,
                code
            ) as MoveFile
        return dummyFile.descendantOfTypeStrict()
    }
}