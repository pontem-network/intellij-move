package org.move.ide.presentation

import com.intellij.ide.projectView.PresentationData
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvDocAndAttributeOwner
import org.move.lang.core.types.address
import org.move.lang.core.types.fqName
import org.move.lang.toNioPathOrNull
import org.move.openapiext.rootPath
import java.nio.file.Path

fun getPresentation(psi: PsiElement): ItemPresentation {
    if (psi is MoveFile) {
        return psi.presentation!!
    }
    val location = psi.locationString(tryRelative = true)
    val name = presentableName(psi)
    return PresentationData(name, location, psi.getIcon(0), null)
}

fun getPresentationForStructure(psi: PsiElement): ItemPresentation {
    if (psi is MoveFile) {
        return psi.presentation!!
    }
    val presentation = buildString {
        fun appendCommaList(xs: List<String>) {
            append('(')
            append(xs.joinToString(", "))
            append(')')
        }
        append(presentableName(psi))
        when (psi) {
            is MvFunctionLike -> {
                append(psi.signatureText)
            }
            is MvConst -> {
                psi.type?.let { append(": ${it.text}") }
            }
            is MvNamedFieldDecl -> {
                psi.type?.let { append(": ${it.text}") }
            }
            is MvEnumVariant -> {
                val fields = psi.tupleFields
                if (fields != null) {
                    appendCommaList(fields.tupleFieldDeclList.map { it.type.text })
                }
            }
        }
    }
    val icon = psi.getIcon(Iconable.ICON_FLAG_VISIBILITY)
    return PresentationData(presentation, null, icon, null)
}

fun PsiElement.locationString(tryRelative: Boolean): String? = when (this) {
    is MvModule -> {
        this.address()?.normalizedText() ?: ""
    }
    else -> containingFilePath(tryRelative)?.toString()
}

val MvDocAndAttributeOwner.presentableQualifiedName: String?
    get() {
        val fqName = (this as? MvNamedElement)?.fqName()?.identifierText()
        if (fqName != null) return fqName
        return name
    }

private fun PsiElement.containingFilePath(tryRelative: Boolean): Path? {
    val containingFilePath = this.containingFile.toNioPathOrNull() ?: return null
    if (tryRelative) {
        val rootPath = this.project.rootPath
        if (rootPath != null) {
            return rootPath.relativize(containingFilePath)
        }
    }
    return containingFilePath
}

private fun presentableName(psi: PsiElement): String? {
    return when (psi) {
        is MvNamedElement -> psi.name
        else -> null
    }
}