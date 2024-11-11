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
import org.move.ide.presentation.presentationInfo
import org.move.ide.presentation.text
import org.move.ide.presentation.typeLabel
import org.move.lang.core.psi.MvAbility
import org.move.lang.core.psi.MvConst
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvFunctionParameter
import org.move.lang.core.psi.MvFunctionParameterList
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNamedAddress
import org.move.lang.core.psi.MvNamedFieldDecl
import org.move.lang.core.psi.MvPatBinding
import org.move.lang.core.psi.MvReturnType
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvType
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.psi.MvTypeParameterList
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.isNative
import org.move.lang.core.psi.module
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.moveProject
import org.move.stdext.joinToWithBuffer
import org.toml.lang.psi.TomlKeySegment

class MvPsiDocumentationTargetProvider : PsiDocumentationTargetProvider {
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
class MvDocumentationTarget(val element: PsiElement, private val originalElement: PsiElement?) : DocumentationTarget {
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
            is MvNamedAddress -> {
                // TODO: add docs for both [addresses] and [dev-addresses]
                val moveProject = docElement.moveProject ?: return null
                val refName = docElement.referenceName
                val named = moveProject.getNamedAddressTestAware(refName) ?: return null
                val address =
                    named.addressLit()?.original ?: angleWrapped("unassigned")
                return "$refName = \"$address\""
            }
            is MvDocAndAttributeOwner -> generateOwnerDoc(docElement, buffer)
            is MvPatBinding -> {
                val presentationInfo = docElement.presentationInfo ?: return null
                val msl = docElement.isMslOnlyItem
                val inference = docElement.inference(msl) ?: return null
                val type = inference.getBindingType(docElement).renderForDocs(true)
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
                val abilities = docElement.abilityBounds
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
    val commentText = docComments().map { it.text }.joinToString("\n")
    return documentationAsHtml(commentText, this)
}

fun generateFunction(function: MvFunction, buffer: StringBuilder) {
    val module = function.module
    if (module != null) {
        buffer += module.qualName?.editorText() ?: "unknown"
        buffer += "\n"
    }
    if (function.isNative) buffer += "native "
    buffer += "fun "
    buffer.b { it += function.name }
    function.typeParameterList?.generateDocumentation(buffer)
    function.functionParameterList?.generateDocumentation(buffer)
    function.returnType?.generateDocumentation(buffer)
}

fun MvElement.signature(builder: StringBuilder) {
    val buffer = StringBuilder()
    // no need for msl type conversion in docs
    val msl = false
//    val msl = this.isMslLegacy()
    when (this) {
        is MvFunction -> generateFunction(this, buffer)
        is MvModule -> {
            buffer += "module "
            buffer += this.qualName?.editorText() ?: "unknown"
        }

        is MvStruct -> {
            buffer += this.module.qualName?.editorText() ?: "unknown"
            buffer += "\n"

            buffer += "struct "
            buffer.b { it += this.name }
            this.typeParameterList?.generateDocumentation(buffer)
            this.abilitiesList?.abilityList
                ?.joinToWithBuffer(buffer, ", ", " has ") { generateDocumentation(it) }
        }

        is MvNamedFieldDecl -> {
            val module = this.fieldOwner.itemElement.module
//            val itemContext = this.structItem.outerItemContext(msl)
            buffer += module.qualName?.editorText() ?: "unknown"
            buffer += "::"
            buffer += this.fieldOwner.name ?: angleWrapped("anonymous")
            buffer += "\n"
            buffer.b { it += this.name }
            buffer += ": ${(this.type?.loweredType(msl) ?: TyUnknown).renderForDocs(true)}"
//            buffer += ": ${itemContext.getStructFieldItemTy(this).renderForDocs(true)}"
        }

        is MvConst -> {
//            val itemContext = this.outerItemContext(msl)
            buffer += this.module?.qualName?.editorText() ?: angleWrapped("unknown")
            buffer += "\n"
            buffer += "const "
            buffer.b { it += this.name ?: angleWrapped("unknown") }
            buffer += ": ${(this.type?.loweredType(msl) ?: TyUnknown).renderForDocs(false)}"
//            buffer += ": ${itemContext.getConstTy(this).renderForDocs(false)}"
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
            val msl = this.isMsl()
            buffer += this.loweredType(msl)
                .typeLabel(this)
                .replace("<", "&lt;")
                .replace(">", "&gt;")
        }

        is MvFunctionParameterList ->
            this.functionParameterList
                .joinToWithBuffer(buffer, ", ", "(", ")") { generateDocumentation(it) }

        is MvFunctionParameter -> {
            buffer += this.patBinding.identifier.text
            this.type?.generateDocumentation(buffer, ": ")
        }

        is MvTypeParameterList ->
            this.typeParameterList
                .joinToWithBuffer(buffer, ", ", "&lt;", "&gt;") { generateDocumentation(it) }

        is MvTypeParameter -> {
            if (this.isPhantom) {
                buffer += "phantom"
                buffer += " "
            }
            buffer += this.identifier?.text
            val bound = this.typeParamBound
            if (bound != null) {
                abilityBounds.joinToWithBuffer(buffer, " + ", ": ") { generateDocumentation(it) }
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
    val original = this.text(fq)
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
