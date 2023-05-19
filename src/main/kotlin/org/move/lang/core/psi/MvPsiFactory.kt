package org.move.lang.core.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiParserFacade
import org.intellij.lang.annotations.Language
import org.move.ide.utils.FunctionSignature
import org.move.lang.MoveFile
import org.move.lang.MoveFileType
import org.move.lang.core.psi.ext.childOfType
import org.move.lang.core.psi.ext.descendantOfTypeStrict
import org.move.lang.core.psi.ext.resolveContext
import org.move.lang.core.psi.impl.MvFunctionParameterImpl

val Project.psiFactory get() = MvPsiFactory(this)

class MvPsiFactory(val project: Project) {
    fun itemSpecSignature(signature: FunctionSignature): MvItemSpecSignature {
        val typeParams = signature.typeParameters
        val typeParamsText =
            if (typeParams.isNotEmpty())
                signature.typeParameters.joinToString(", ", "<", ">")
            else ""
        val paramsText = signature.parameters.joinToString(", ", "(", ")")
        return createFromText("spec 0x1::_M { spec call$typeParamsText$paramsText }")
            ?: error("Failed to create item spec signature")
    }

    fun structLitField(fieldName: String, expr: String): MvStructLitField =
        createFromText("module _M { fun m() { S { $fieldName: $expr }; }}")
            ?: error("Failed to create MvStructLitField")

    fun structPatField(fieldName: String, alias: String): MvStructPatField =
        createFromText("module _M { fun m() { let S { $fieldName: $alias } = 1; }}")
            ?: error("Failed to create MvStructPatField")

    fun schemaLitField(fieldName: String, expr: String): MvSchemaLitField =
        createFromText("module _M { spec module { include Schema { $fieldName: $expr } }}")
            ?: error("Failed to create MvSchemaField")

    fun inlineModule(address: String, name: String, blockText: String): MvModule =
        createFromText("module $address::$name $blockText") ?: error("failed to create module")

    fun abilitiesList(names: List<String>): MvAbilitiesList =
        createFromText("module 0x1::main { struct S has ${names.joinToString(", ")} {} }")
            ?: error("failed to create abilities")

    fun ability(name: String): MvAbility =
        createFromText("module 0x1::main { struct S has $name {} }") ?: error("failed to create ability")

    fun identifier(text: String): PsiElement =
        createFromText<MvModule>("module $text {}")?.nameIdentifier
            ?: error("Failed to create identifier: `$text`")

//    fun createColon(): PsiElement =
//        const("const C: u8 = 1;")
//            .descendantOfTypeStrict<MvTypeAnnotation>()!!
//            .getChild(MvElementTypes.COLON)!!

//    fun createExpression(text: String): MvExpr =
//        tryCreateExpression(text)
//            ?: error("Failed to create expression from text: `$text`")
//
//    fun tryCreateExpression(text: CharSequence): MvExpr? =
//        createFromText("module 0x1::_DummyModule { fun m() { let _ = $text; } }")

    fun const(text: String): MvConst =
        createFromText("module 0x1::_DummyModule { $text }")
            ?: error("Failed to create const")

    @Suppress("InvalidModuleDeclaration")
    fun builtinConst(text: String): MvConst =
        createFromText("module spec_builtins { $text }") ?: error("Failed to create const")

    inline fun <reified T : MvExpr> expr(text: String): T =
        createFromText("module 0x1::_DummyModule { fun call() { let _ = $text; } }")
            ?: error("Failed to create expr")

    fun useStmt(speckText: String, testOnly: Boolean): MvUseStmt {
        return createFromText("module 0x1::_DummyModule { ${if (testOnly) "#[test_only]\n" else ""}use $speckText; }")
            ?: error("Failed to create an item import from text: `$speckText`")
    }

    fun itemUseSpeck(fqModuleText: String, names: List<String>): MvItemUseSpeck {
        assert(names.isNotEmpty())
        return if (names.size == 1) {
            createFromText("module 0x1::_DummyModule { use $fqModuleText::${names.first()}; }")
                ?: error("")
        } else {
            val namesText = names.joinToString(", ", "{", "}")
            createFromText("module 0x1::_DummyModule { use $fqModuleText::$namesText; }")
                ?: error("")
        }
    }

    fun useItemGroup(names: List<String>): MvUseItemGroup {
        val namesText = names.joinToString(", ")
        return createFromText("module 0x1::_DummyModule { use 0x1::Module::{$namesText}; }")
            ?: error("Failed to create an item import from text: `$namesText`")
    }

    fun useItem(text: String): MvUseItem {
        return createFromText("module 0x1::_DummyModule { use 0x1::Module::$text; }")
            ?: error("Failed to create an item import from text: `$text`")
    }

    fun acquires(text: String): MvAcquiresType {
        return createFromText("module 0x1::_DummyModule { fun main() $text {}}")
            ?: error("Failed to create a method member from text: `$text`")
    }

    fun bindingPat(text: String): MvBindingPat {
        return createFromText("module 0x1::_DummyModule { fun main() { let S { $text } = 1; }}")
            ?: error("Failed to create a MvBindingPat from text: `$text`")
    }

    @Suppress("InvalidModuleDeclaration")
    fun specFunctionParameter(parent: MvFunction, name: String, type: String): MvFunctionParameter {
        val parameter = createFromText<MvFunctionParameter>(
            """
            module spec_builtins {
                spec module {
                    fun _IntellijDummy($name: $type)
                }
            }
            """.trimIndent()
        ) ?: error("Failed to create a MvFunctionParameter with name = $name, type = $type")
        parameter.resolveContext = parent
        (parameter as MvFunctionParameterImpl).resolveContext = parent
        return parameter
    }

    fun typeParameter(text: String): MvTypeParameter {
        return createFromText("module 0x1::_DummyModule { struct S<$text> {}}")
            ?: error("Failed to create a type parameter from text: `$text`")
    }

    fun path(text: String): MvPath {
        return createFromText("module 0x1::_DummyModule { fun main() { $text(); } } ")
            ?: error("`$text`")
    }

    fun function(text: String, moduleName: String = "_Dummy"): MvFunction =
        createFromText("module $moduleName { $text } ")
            ?: error("Failed to create a function from text: `$text`")

    fun functions(text: String, moduleName: String = "_Dummy"): List<MvFunction> {
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText(
                "$moduleName.move",
                MoveFileType,
                "module $moduleName { $text }"
            ) as MoveFile
        val functions = dummyFile.childOfType<MvModule>()?.moduleBlock?.functionList.orEmpty()
        return functions
    }

    fun addressRef(text: String): MvAddressRef =
        createFromText("module $text::Main {} ")
            ?: error("Failed to create a function from text: `$text`")

    fun specFunction(text: String, moduleName: String = "_Dummy"): MvSpecFunction =
        createFromText("module $moduleName { $text } ")
            ?: error("Failed to create a function from text: `$text`")

    fun createWhitespace(ws: String): PsiElement =
        PsiParserFacade.getInstance(project).createWhiteSpaceFromText(ws)

    fun createNewline(): PsiElement = createWhitespace("\n")

    inline fun <reified T : MvElement> createFromText(@Language("Move") code: CharSequence): T? {
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText(
                "DUMMY.move",
                MoveFileType,
                code
            ) as MoveFile
        return dummyFile.descendantOfTypeStrict()
    }
}
