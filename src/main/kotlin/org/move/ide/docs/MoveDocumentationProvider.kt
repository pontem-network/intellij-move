package org.move.ide.docs

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MoveDocAndAttributeOwner
import org.move.lang.core.psi.ext.addressValue
import org.move.lang.core.types.HasType
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
            is MoveNamedAddress -> return docElement.addressValue
            is MoveDocAndAttributeOwner -> generateOwnerDoc(docElement, buffer)
            else -> {
                if (docElement !is HasType) return null
                val type = docElement.resolvedType(emptyMap()) ?: return null
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

fun MoveElement.signature(builder: StringBuilder) {
    val funcSignature = when (this) {
        is MoveFunctionDef -> this.functionSignature
        is MoveNativeFunctionDef -> this.functionSignature
        else -> return
    } ?: return

    val buffer = StringBuilder()
    buffer.b { it += funcSignature.name }
    funcSignature.functionParameterList?.generateDocumentation(buffer)
    funcSignature.returnType?.generateDocumentation(buffer)
    val rawLines = listOf(buffer.toString())

//    val rawLines = when (this) {
//        is MoveFunctionDef, is MoveNativeFunctionDef -> {
//            val funcSignature = this.functi
//            val buffer = StringBuilder()
//            buffer.b { it += name }
//            buffer += "()"
//            returnType?.generateDocumentation(buffer)
//            listOf(buffer.toString())
//        }
//        else -> emptyList()
//    }
    rawLines.joinTo(builder, "<br>")
}

private fun PsiElement.generateDocumentation(buffer: StringBuilder, prefix: String = "", suffix: String = "") {
    buffer += prefix
    when (this) {
        is MoveType -> {
            buffer += this.resolvedType(emptyMap())?.typeLabel(this) ?: "<unknown>"
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
