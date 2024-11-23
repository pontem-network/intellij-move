package org.move.ide.docs

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.model.Pointer
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer
import org.move.ide.docs.MvColorUtils.asAbility
import org.move.ide.docs.MvColorUtils.asTypeParam
import org.move.ide.docs.MvColorUtils.colored
import org.move.ide.docs.MvColorUtils.keyword
import org.move.ide.presentation.declaringModule
import org.move.ide.presentation.presentationInfo
import org.move.ide.presentation.text
import org.move.lang.core.psi.MvAbility
import org.move.lang.core.psi.MvConst
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvFunctionParameter
import org.move.lang.core.psi.MvFunctionParameterList
import org.move.lang.core.psi.MvNamedAddress
import org.move.lang.core.psi.MvPatBinding
import org.move.lang.core.psi.MvReturnType
import org.move.lang.core.psi.MvType
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.psi.MvTypeParameterList
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.loweredType
import org.move.lang.moveProject
import org.move.stdext.joinToWithBuffer
import org.toml.lang.psi.TomlKeySegment
import kotlin.collections.isNotEmpty

class MvPsiDocumentationTargetProvider: PsiDocumentationTargetProvider {
    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        if (element is MvElement) {
            return MvDocumentationTarget(element, originalElement)
        }
        if (element is TomlKeySegment) {
            val namedAddress = originalElement?.ancestorOrSelf<MvNamedAddress>()
            if (namedAddress != null) return MvDocumentationTarget(namedAddress, originalElement)
            return MvDocumentationTarget(element, originalElement)
        }
        return null
    }
}

@Suppress("UnstableApiUsage")
class MvDocumentationTarget(
    val element: PsiElement,
    private val originalElement: PsiElement?
): DocumentationTarget {
    override fun computePresentation(): TargetPresentation {
        val project = element.project
        val file = element.containingFile?.virtualFile

        return TargetPresentation.builder("")
            .backgroundColor(file?.let { VfsPresentationUtil.getFileBackgroundColor(project, file) })
            .presentation()
    }

    override fun computeDocumentation(): DocumentationResult? {
        val content = generateDoc(element) ?: return null
        return DocumentationResult.documentation(content)
    }

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPtr = element.createSmartPointer()
        val originalElementPtr = originalElement?.createSmartPointer()
        return Pointer {
            val element = elementPtr.dereference() ?: return@Pointer null
            MvDocumentationTarget(element, originalElementPtr?.dereference())
        }
    }

    fun generateDoc(element: PsiElement?): String? {
        val buffer = StringBuilder()

        var docElement = element
        if (docElement is MvPatBinding && docElement.bindingTypeOwner is MvConst) {
            docElement = docElement.bindingTypeOwner
        }

        when (docElement) {
            is MvDocAndAttributeOwner -> generateDocumentationOwnerDoc(docElement, buffer)
            is MvNamedAddress -> {
                val moveProject = docElement.moveProject ?: return null
                val refName = docElement.referenceName
                val named = moveProject.getNamedAddressTestAware(refName) ?: return null
                val address =
                    named.addressLit()?.original ?: angleWrapped("unassigned")
                return "$refName = \"$address\""
            }
            is MvPatBinding -> {
                val presentationInfo = docElement.presentationInfo ?: return null
                buffer.keyword(presentationInfo.type)
                buffer += " "
                buffer += presentationInfo.name
                val msl = docElement.isMslOnlyItem
                val inference = docElement.inference(msl) ?: return null
                val tyText = inference
                    .getBindingType(docElement)
                    .text(fq = true, colors = true)
                buffer += ": "
                buffer += tyText
            }

            is MvTypeParameter -> {
                val presentationInfo = docElement.presentationInfo ?: return null
                buffer += presentationInfo.type
                buffer += " "
                buffer.b { it += presentationInfo.name }
                val abilities = docElement.abilityBounds
                if (abilities.isNotEmpty()) {
                    abilities.joinToWithBuffer(buffer, " + ", ": ") { generateDoc(it) }
                }
            }
        }
        return if (buffer.isEmpty()) null else buffer.toString()
    }

    private fun generateDocumentationOwnerDoc(element: MvDocAndAttributeOwner, buffer: StringBuilder) {
        definition(buffer) {
            element.header(it)
            element.signature(it)
        }
        val text = element.documentationAsHtml()
        if (text.isEmpty()) return
        buffer += "\n" // Just for more pretty html text representation
        content(buffer) { it += text }
    }
}

fun MvDocAndAttributeOwner.documentationAsHtml(): String {
    val commentText = docComments().map { it.text }.joinToString("\n")
    return documentationAsHtml(commentText, this)
}

fun PsiElement.generateDoc(buf: StringBuilder) {
    when (this) {
        is MvType -> {
            val msl = this.isMsl()
            val ty = this.loweredType(msl)

            val tyItemModule = ty.declaringModule()
            val fq = tyItemModule != null && tyItemModule != this.containingModule
            buf += ty.text(fq, colors = true)
        }

        is MvFunctionParameterList ->
            this.functionParameterList
                .joinToWithBuffer(buf, ", ", "(", ")") { generateDoc(it) }

        is MvFunctionParameter -> {
            buf += this.patBinding.identifier.text
            this.type?.let {
                buf += ": "
                it.generateDoc(buf)
            }
        }

        is MvTypeParameterList ->
            this.typeParameterList
                .joinToWithBuffer(buf, ", ", "&lt;", "&gt;") { generateDoc(it) }

        is MvTypeParameter -> {
            if (this.isPhantom) {
                buf.keyword("phantom")
                buf += " "
            }
            buf.colored(this.name, asTypeParam)
            val bound = this.typeParamBound
            if (bound != null) {
                abilityBounds.joinToWithBuffer(buf, " + ", ": ") { generateDoc(it) }
            }
        }
        is MvAbility -> buf.colored(this.text, asAbility)
        is MvReturnType -> this.type?.let {
            buf += ": "
            it.generateDoc(buf)
        }
    }
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

fun String.escapeForHtml(): String {
    return this
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}

operator fun StringBuilder.plusAssign(value: String?) {
    if (value != null) {
        append(value)
    }
}

private inline fun StringBuilder.b(action: (StringBuilder) -> Unit) {
    append("<b>")
    action(this)
    append("</b>")
}

private fun angleWrapped(text: String): String = "&lt;$text&gt;"
