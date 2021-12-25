package org.move.lang.core.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.move.lang.MvFile
import org.move.lang.MoveFileType
import org.move.lang.core.psi.ext.descendantOfTypeStrict

val Project.psiFactory get() = MvPsiFactory(this)

class MvPsiFactory(private val project: Project) {
    fun createIdentifier(text: String): PsiElement =
        createFromText<MvModuleDef>("module $text {}")?.nameIdentifier
            ?: error("Failed to create identifier: `$text`")

//    fun createQualifiedPath(text: String): MvQualPath =
//        createFromText("script { fun main() { $text; } }") ?: error("Failed to create QualifiedPath")

    fun createFunctionSignature(text: String): MvFunctionSignature {
        return createFromText("module _IntellijPreludeDummy { $text {}}")
            ?: error("Failed to create a method member from text: `$text`")
    }

    fun createItemImport(text: String): MvItemImport {
        return createFromText("module _IntellijPreludeDummy { use 0x1::Module::$text; }")
            ?: error("Failed to create an item import from text: `$text`")
    }

    fun createAcquiresType(text: String): MvAcquiresType {
        return createFromText("module _IntellijPreludeDummy { fun main() $text {}}")
            ?: error("Failed to create a method member from text: `$text`")
    }

    fun createPathIdent(text: String): MvPathIdent {
        return createFromText("module _IntellijPreludeDummy { fun main() { $text(); } } ")
            ?: error("`$text`")
    }

    fun createFunction(text: String, moduleName: String = "_Dummy"): MvFunction =
        createFromText("module $moduleName { $text } ")
            ?: error("Failed to create a function from text: `$text`")

//    fun createNativeFunctionDef(
//        text: String,
//        moduleName: String = "_IntellijPreludeDummy"
//    ): MvNativeFunctionDef =
//        createFromText("module $moduleName { $text }")
//            ?: error("Failed to create a method member from text: `$text`")

    private inline fun <reified T : MvElement> createFromText(code: CharSequence): T? {
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText(
                "DUMMY.move",
                MoveFileType,
                code
            ) as MvFile
        return dummyFile.descendantOfTypeStrict()
    }
}
