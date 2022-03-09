package org.move.lang.core.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.move.lang.MvFile
import org.move.lang.MoveFileType
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.ext.descendantOfTypeStrict
import org.move.lang.core.psi.ext.getChild

val Project.psiFactory get() = MvPsiFactory(this)

class MvPsiFactory(private val project: Project) {
    fun createStructLitField(name: String, value: String): MvStructLitField =
        createFromText("module _M { fun m() { S { $name: $value }; }}")
            ?: error("Failed to create MvStructLitField")

    fun createStructPatField(name: String, alias: String): MvStructPatField =
        createFromText("module _M { fun m() { let S { $name: $alias } = 1; }}")
            ?: error("Failed to create MvStructPatField")

    fun createSchemaLitField(fieldName: String, expr: String): MvSchemaField =
        createFromText("module _M { spec module { include Schema { $fieldName: $expr } }}")
            ?: error("Failed to create MvSchemaField")

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

    fun createItemImport(text: String): MvItemImport {
        return createFromText("module _IntellijPreludeDummy { use 0x1::Module::$text; }")
            ?: error("Failed to create an item import from text: `$text`")
    }

    fun createAcquires(text: String): MvAcquiresType {
        return createFromText("module _IntellijPreludeDummy { fun main() $text {}}")
            ?: error("Failed to create a method member from text: `$text`")
    }

    fun createBindingPat(text: String): MvBindingPat {
        return createFromText("module _IntellijPreludeDummy { fun main() { let S { $text } = 1; }}")
            ?: error("Failed to create a MvBindingPat from text: `$text`")
    }

    fun createTypeParameter(text: String): MvTypeParameter {
        return createFromText("module _IntellijPreludeDummy { struct S<$text> {}}")
            ?: error("Failed to create a type parameter from text: `$text`")
    }

    fun createPathIdent(text: String): MvPathIdent {
        return createFromText("module _IntellijPreludeDummy { fun main() { $text(); } } ")
            ?: error("`$text`")
    }

    fun createFunction(text: String, moduleName: String = "_Dummy"): MvFunction =
        createFromText("module $moduleName { $text } ")
            ?: error("Failed to create a function from text: `$text`")

    fun createSpecFunction(text: String, moduleName: String = "_Dummy"): MvSpecFunction =
        createFromText("module $moduleName { $text } ")
            ?: error("Failed to create a function from text: `$text`")

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
