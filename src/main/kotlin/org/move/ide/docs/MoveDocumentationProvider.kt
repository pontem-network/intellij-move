package org.move.ide.docs

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.ide.presentation.shortPresentableText
import org.move.ide.presentation.typeLabel
import org.move.lang.containingMoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MoveDocAndAttributeOwner
import org.move.lang.core.psi.ext.fqName
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.ext.structDef
import org.move.lang.core.psi.mixins.isNative
import org.move.lang.core.types.ty.HasType
import org.move.lang.core.types.ty.TyUnknown
import org.move.stdext.joinToWithBuffer

class MoveDocumentationProvider : AbstractDocumentationProvider() {
    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if (contextElement is MoveNamedAddress) return contextElement
        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset)
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val buffer = StringBuilder()
        var docElement = element
        if (docElement is MoveFunctionSignature) docElement = docElement.parent
        when (docElement) {
            // TODO: add docs for both scopes
            is MoveNamedAddress -> {
                val moveProject = docElement.containingFile.containingMoveProject() ?: return null
                val refName = docElement.referenceName ?: return null
//                return moveProject.getNormalizedAddressValue(refName)
                return moveProject.getAddresses()[refName]
            }
            is MoveDocAndAttributeOwner -> generateOwnerDoc(docElement, buffer)
            else -> {
                if (docElement !is HasType) return null
                val type = docElement.resolvedType()
                buffer += type.typeLabel(docElement)
            }
        }
        return if (buffer.isEmpty()) null else buffer.toString()
    }

    private fun generateOwnerDoc(element: MoveDocAndAttributeOwner, buffer: StringBuilder) {
        definition(buffer) {
            element.signature(it)
        }
        val text = element.documentationAsHtml()
        buffer += "\n" // Just for more pretty html text representation
        content(buffer) { it += text }
    }
}

fun MoveDocAndAttributeOwner.documentationAsHtml(): String {
    return docComments()
        .flatMap { it.text.split("\n") }
        .map { it.trimStart('/', ' ') }
        .map { "<p>$it</p>" }
        .joinToString("\n")
}

fun generateFunctionSignature(signature: MoveFunctionSignature, buffer: StringBuilder) {
    val module = signature.module
    if (module != null) {
        buffer += module.fqName
        buffer += "\n"
    }
    if (signature.isNative) buffer += "native "
    buffer += "fun "
    buffer.b { it += signature.name }
    signature.functionParameterList?.generateDocumentation(buffer)
    signature.returnType?.generateDocumentation(buffer)
}

fun MoveElement.signature(builder: StringBuilder) {
    val buffer = StringBuilder()
    when (this) {
        is MoveFunctionDef -> this.functionSignature?.let { generateFunctionSignature(it, buffer) }
        is MoveNativeFunctionDef -> this.functionSignature?.let { generateFunctionSignature(it, buffer) }
        is MoveModuleDef -> {
            buffer += "module "
            buffer += this.fqName
        }
        is MoveStructFieldDef -> {
            buffer += this.containingModule!!.fqName
            buffer += "::"
            buffer += this.structDef?.structSignature?.name ?: "<anonymous>"
            buffer += "\n"
            buffer.b { it += this.name }
            this.typeAnnotation?.let {
                val type = it.type?.resolvedType() ?: TyUnknown
                buffer += ": ${type.shortPresentableText(true)}"
            }
        }
        is MoveConstDef -> {
            buffer += this.containingModule!!.fqName
            buffer += "\n"
            buffer += "const "
            buffer.b { it += this.name }
            buffer += ": ${this.resolvedType().shortPresentableText(false)}"
            this.initializer?.let { buffer += " ${it.text}" }
        }
        else -> return
    } ?: return
    listOf(buffer.toString()).joinTo(builder, "<br>")
}

private fun PsiElement.generateDocumentation(buffer: StringBuilder, prefix: String = "", suffix: String = "") {
    buffer += prefix
    when (this) {
        is MoveType -> {
            buffer += this.resolvedType().typeLabel(this)
        }
        is MoveFunctionParameterList ->
            this.functionParameterList
                .joinToWithBuffer(buffer, ", ", "(", ")") { generateDocumentation(it) }
        is MoveFunctionParameter -> {
            buffer += this.identifier.text
            this.typeAnnotation?.type?.generateDocumentation(buffer, ": ")
        }
        is MoveReturnType -> this.type?.generateDocumentation(buffer, ": ")
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
