package org.move.lang.core.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.move.lang.MvFile
import org.move.lang.MoveFileType
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.ext.childOfType
import org.move.lang.core.psi.ext.descendantOfTypeStrict
import org.move.lang.core.psi.ext.getChild
import org.move.lang.core.psi.ext.hasChild

val Project.psiFactory get() = MvPsiFactory(this)

class MvPsiFactory(private val project: Project) {
    fun createStructLitField(name: String, value: String): MvStructLitField =
        createFromText("module _M { fun m() { S { $name: $value }; }}")
            ?: error("Failed to create MvStructLitField")

    fun createStructPatField(name: String, alias: String): MvStructPatField =
        createFromText("module _M { fun m() { let S { $name: $alias } = 1; }}")
            ?: error("Failed to create MvStructPatField")

//    fun createFieldInit(text: String): MvFieldInit =
//        createFromText("module _M { fun m() { S { myfield $text }; }}") ?: error("")

    fun createIdentifier(text: String): PsiElement =
        createFromText<MvModuleDef>("module $text {}")?.nameIdentifier
            ?: error("Failed to create identifier: `$text`")

    fun createColon(): PsiElement =
        createConst("const C: u8 = 1;")
            .descendantOfTypeStrict<MvTypeAnnotation>()!!
            .getChild(MvElementTypes.COLON)!!

    fun createExpression(text: String): MvExpr =
        tryCreateExpression(text)
            ?: error("Failed to create expression from text: `$text`")

    fun tryCreateExpression(text: CharSequence): MvExpr? =
        createFromText("module _IntellijPreludeDummy { fun m() { let _ = $text; } }")

    fun createConst(text: String): MvConstDef =
        createFromText("module _IntellijPreludeDummy { $text }")
            ?: error("")
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
