package org.move.toml

import com.intellij.psi.PsiComment
import org.move.lang.core.psi.ext.nextNonWsSibling
import org.move.openapiext.line
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlTableHeader

val TomlKeySegment.isDependencyKey: Boolean
    get() {
        val name = name
        return name == "dependencies" || name == "dev-dependencies"
    }

val TomlTableHeader.isDependencyListHeader: Boolean
    get() = key?.segments?.lastOrNull()?.isDependencyKey == true

val TomlKeySegment.isAddressesKey: Boolean
    get() {
        val name = name
        return name == "addresses" || name == "dev-addresses"
    }

fun TomlKeySegment.findInlineDocumentation(): String? {
    val keyValue = parent.parent
    val possibleInlineComment = keyValue.nextNonWsSibling
    if (possibleInlineComment is PsiComment && keyValue.line() == possibleInlineComment.line()) {
        return possibleInlineComment.text.trimStart('#', ' ')
    }
    return null
}

val TomlTableHeader.isAddressesListHeader: Boolean
    get() = key?.segments?.lastOrNull()?.isDependencyKey == true
