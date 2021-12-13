package org.move.ide.docs

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.ide.presentation.presentationInfo
import org.move.ide.presentation.shortPresentableText
import org.move.ide.presentation.typeLabel
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.mixins.isNative
import org.move.lang.core.types.infer.inferMvTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.moveProject
import org.move.stdext.joinToWithBuffer

class MvDocumentationProvider : AbstractDocumentationProvider() {
    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if (contextElement is MvNamedAddress) return contextElement
        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset)
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val buffer = StringBuilder()
        var docElement = element
        if (docElement is MvFunctionSignature) docElement = docElement.parent
        if (docElement is MvStructSignature) docElement = docElement.parent
        if (docElement is MvBindingPat
            && docElement.owner is MvConstDef
        ) docElement = docElement.owner
        when (docElement) {
            // TODO: add docs for both scopes
            is MvNamedAddress -> {
                val moveProject = docElement.moveProject ?: return null
                val refName = docElement.referenceName ?: return null
                return moveProject.getAddressValue(refName)
            }
            is MvDocAndAttributeOwner -> generateOwnerDoc(docElement, buffer)
            is MvBindingPat -> {
                val presentationInfo = docElement.presentationInfo ?: return null
                val type = docElement.inferBindingPatTy().renderForDocs(true)
                buffer += presentationInfo.type
                buffer += " "
                buffer.b { it += presentationInfo.name }
                buffer += ": "
                buffer += type
            }
            is MvTypeParameter -> {
                val presentationInfo = docElement.presentationInfo ?: return null
                buffer += presentationInfo.type
                buffer += " "
                buffer.b { it += presentationInfo.name }
                val abilities = docElement.abilities
                if (abilities.isNotEmpty()) {
                    abilities.joinToWithBuffer(buffer, " + ", ": ") { generateDocumentation(it) }
                }
            }
        }
        return if (buffer.isEmpty()) null else buffer.toString()
    }

    private fun generateOwnerDoc(element: MvDocAndAttributeOwner, buffer: StringBuilder) {
        definition(buffer) {
            element.signature(it)
        }
        val text = element.documentationAsHtml()
        buffer += "\n" // Just for more pretty html text representation
        content(buffer) { it += text }
    }
}

fun MvDocAndAttributeOwner.documentationAsHtml(): String {
    return docComments()
        .flatMap { it.text.split("\n") }
        .map { it.trimStart('/', ' ') }
        .map { "<p>$it</p>" }
        .joinToString("\n")
}

fun generateFunctionSignature(signature: MvFunctionSignature, buffer: StringBuilder) {
    val module = signature.module
    if (module != null) {
        buffer += module.fqName
        buffer += "\n"
    }
    if (signature.isNative) buffer += "native "
    buffer += "fun "
    buffer.b { it += signature.name }
    signature.typeParameterList?.generateDocumentation(buffer)
    signature.functionParameterList?.generateDocumentation(buffer)
    signature.returnType?.generateDocumentation(buffer)
}

fun MvElement.signature(builder: StringBuilder) {
    val buffer = StringBuilder()
    when (this) {
        is MvFunctionDef -> this.functionSignature?.let { generateFunctionSignature(it, buffer) }
        is MvNativeFunctionDef -> this.functionSignature?.let { generateFunctionSignature(it, buffer) }
        is MvModuleDef -> {
            buffer += "module "
            buffer += this.fqName
        }
        is MvStructDef -> {
            buffer += this.containingModule!!.fqName
            buffer += "\n"

            val signature = this.structSignature
            buffer += "struct "
            buffer.b { it += signature.name }
            signature.typeParameterList?.generateDocumentation(buffer)
            signature.abilitiesList?.abilityList
                ?.joinToWithBuffer(buffer, ", ", " has ") { generateDocumentation(it) }
        }
        is MvStructFieldDef -> {
            buffer += this.containingModule!!.fqName
            buffer += "::"
            buffer += this.structDef?.structSignature?.name ?: angleWrapped("anonymous")
            buffer += "\n"
            buffer.b { it += this.name }
            buffer += ": ${this.declaredTy.renderForDocs(true)}"
        }
        is MvConstDef -> {
            buffer += this.containingModule!!.fqName
            buffer += "\n"
            buffer += "const "
            buffer.b { it += this.bindingPat?.name ?: angleWrapped("unknown") }
            buffer += ": ${this.declaredTy.renderForDocs(false)}"
            this.initializer?.let { buffer += " ${it.text}" }
        }
        else -> return
    } ?: return
    listOf(buffer.toString()).joinTo(builder, "<br>")
}

private fun PsiElement.generateDocumentation(
    buffer: StringBuilder,
    prefix: String = "",
    suffix: String = ""
) {
    buffer += prefix
    when (this) {
        is MvType -> {
            buffer += inferMvTypeTy(this).typeLabel(this)
        }
        is MvFunctionParameterList ->
            this.functionParameterList
                .joinToWithBuffer(buffer, ", ", "(", ")") { generateDocumentation(it) }
        is MvFunctionParameter -> {
            buffer += this.bindingPat.identifier.text
            this.typeAnnotation?.type?.generateDocumentation(buffer, ": ")
        }
        is MvTypeParameterList ->
            this.typeParameterList
                .joinToWithBuffer(buffer, ", ", "&lt;", "&gt;") { generateDocumentation(it) }
        is MvTypeParameter -> {
            buffer += this.identifier.text
            val bound = this.typeParamBound
            if (bound != null) {
                abilities.joinToWithBuffer(buffer, " + ", ": ") { generateDocumentation(it) }
            }
        }
        is MvAbility -> {
            buffer += this.text
        }
        is MvReturnType -> this.type?.generateDocumentation(buffer, ": ")
    }
    buffer += suffix
}

private inline fun definition(buffer: StringBuilder, block: (StringBuilder) -> Unit) {
    buffer += DocumentationMarkup.DEFINITION_START
    block(buffer)
    buffer += DocumentationMarkup.DEFINITION_END
}

private inline fun content(buffer: StringBuilder, block: (StringBuilder) -> Unit) {
    buffer += DocumentationMarkup.CONTENT_START
    block(buffer)
    buffer += DocumentationMarkup.CONTENT_END
}

private fun angleWrapped(text: String): String = "&lt;$text&gt;"

private fun Ty.renderForDocs(fq: Boolean): String {
    val original = this.shortPresentableText(fq)
    return original
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}

private operator fun StringBuilder.plusAssign(value: String?) {
    if (value != null) {
        append(value)
    }
}

private inline fun StringBuilder.b(action: (StringBuilder) -> Unit) {
    append("<b>")
    action(this)
    append("</b>")
}
