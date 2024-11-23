package org.move.ide.docs

import com.intellij.codeEditor.printing.HTMLTextPainter
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownLeafPsiElement
import org.move.ide.docs.MvColorUtils.asConst
import org.move.ide.docs.MvColorUtils.asEnum
import org.move.ide.docs.MvColorUtils.asEnumVariant
import org.move.ide.docs.MvColorUtils.asField
import org.move.ide.docs.MvColorUtils.asFunction
import org.move.ide.docs.MvColorUtils.asStruct
import org.move.ide.docs.MvColorUtils.colored
import org.move.ide.docs.MvColorUtils.keyword
import org.move.ide.docs.MvColorUtils.op
import org.move.ide.presentation.presentableQualifiedName
import org.move.ide.presentation.text
import org.move.lang.core.psi.MvConst
import org.move.lang.core.psi.MvEnum
import org.move.lang.core.psi.MvEnumVariant
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvFunctionLike
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNamedFieldDecl
import org.move.lang.core.psi.MvSpecFunction
import org.move.lang.core.psi.MvSpecInlineFunction
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.ext.MvDocAndAttributeOwner
import org.move.lang.core.psi.ext.MvStructOrEnumItemElement
import org.move.lang.core.psi.ext.enumItem
import org.move.lang.core.psi.ext.fieldOwner
import org.move.lang.core.psi.ext.modifiers
import org.move.lang.core.types.ty.TyUnknown
import org.move.stdext.joinToWithBuffer

fun MvDocAndAttributeOwner.header(buffer: StringBuilder) {
    val rawLines = when (this) {
        is MvNamedFieldDecl -> listOfNotNull((fieldOwner as? MvDocAndAttributeOwner)?.presentableQualifiedName)
        is MvStructOrEnumItemElement,
        is MvFunctionLike,
        is MvConst -> listOfNotNull(presentableQualifiedModName)
        else -> emptyList()
    }
    rawLines.joinTo(buffer, "<br>")
    if (rawLines.isNotEmpty()) {
        buffer += "\n"
    }
}

fun MvDocAndAttributeOwner.signature(builder: StringBuilder) {
    // no need for msl type conversion in docs
//    val msl = false
    val buffer = StringBuilder()
    when (this) {
        is MvFunction -> buffer.generateFunction(this)
        is MvSpecFunction -> buffer.generateSpecFunction(this)
        is MvSpecInlineFunction -> buffer.generateSpecInlineFunction(this)
        is MvModule -> buffer.generateModule(this)
        is MvStructOrEnumItemElement -> buffer.generateStructOrEnum(this)
        is MvNamedFieldDecl -> buffer.generateNamedField(this)
        is MvConst -> buffer.generateConst(this)
        is MvEnumVariant -> buffer.generateEnumVariant(this)
        else -> return
    }
    listOf(buffer.toString()).joinTo(builder, "<br>")
}

private fun StringBuilder.generateFunction(fn: MvFunction) {
    for (modifier in fn.modifiers) {
        keyword(modifier)
        this += " "
    }
    keyword("fun")
    this += " "
    colored(fn.name, asFunction)

    fn.typeParameterList?.generateDoc(this)
    fn.functionParameterList?.generateDoc(this)
    fn.returnType?.generateDoc(this)
}

private fun StringBuilder.generateSpecFunction(specFn: MvSpecFunction) {
    for (modifier in specFn.modifiers) {
        keyword(modifier)
        this += " "
    }
    keyword("spec")
    this += " "
    keyword("fun")
    this += " "
    colored(specFn.name, asFunction)

    specFn.typeParameterList?.generateDoc(this)
    specFn.functionParameterList?.generateDoc(this)
    specFn.returnType?.generateDoc(this)
}

private fun StringBuilder.generateSpecInlineFunction(specInlineFn: MvSpecInlineFunction) {
    for (modifier in specInlineFn.modifiers) {
        keyword(modifier)
        this += " "
    }
    keyword("fun")
    this += " "
    colored(specInlineFn.name, asFunction)

    specInlineFn.typeParameterList?.generateDoc(this)
    specInlineFn.functionParameterList?.generateDoc(this)
    specInlineFn.returnType?.generateDoc(this)
}

private fun StringBuilder.generateModule(mod: MvModule) {
    keyword("module")
    this += " "
    this += mod.qualName?.editorText() ?: "unknown"
}

private fun StringBuilder.generateStructOrEnum(structOrEnum: MvStructOrEnumItemElement) {
    when (structOrEnum) {
        is MvStruct -> {
            keyword("struct")
            this += " "
            colored(structOrEnum.name, asStruct)
        }
        is MvEnum -> {
            keyword("enum")
            this += " "
            colored(structOrEnum.name, asEnum)
        }
    }
    structOrEnum.typeParameterList?.generateDoc(this)
    structOrEnum.abilitiesList?.abilityList
        ?.joinToWithBuffer(this, ", ", " ${keyword("has")} ") { generateDoc(it) }
}

private fun StringBuilder.generateEnumVariant(variant: MvEnumVariant) {
    this += variant.enumItem.presentableQualifiedName
    this += "::"
    this.colored(variant.name, asEnumVariant)
}

private fun StringBuilder.generateNamedField(field: MvNamedFieldDecl) {
    colored(field.name, asField)
    this += ": "
    val fieldType = field.type
    if (fieldType == null) {
        this += TyUnknown.text(colors = true)
        return
    }
    fieldType.generateDoc(this)
}

private fun StringBuilder.generateConst(const: MvConst) {
    keyword("const")
    this += " "
    colored(const.name, asConst)

    this += ": "
    val constType = const.type
    if (constType == null) {
        this += TyUnknown.text(colors = true)
        return
    }
    constType.generateDoc(this)

    const.initializer?.expr?.let { expr ->
        this += " = "
        this += highlightWithLexer(expr, expr.text)
    }
}

private fun highlightWithLexer(context: PsiElement, text: String): String {
    val highlighed =
        HTMLTextPainter.convertCodeFragmentToHTMLFragmentWithInlineStyles(context, text)
    return highlighed.trimEnd()
        .removeSurrounding("<pre>", "</pre>")
}

private val MvDocAndAttributeOwner.presentableQualifiedModName: String?
    get() = presentableQualifiedName?.removeSuffix("::$name")
