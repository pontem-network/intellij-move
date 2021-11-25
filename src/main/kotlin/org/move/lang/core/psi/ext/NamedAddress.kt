package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import org.move.lang.containingMoveProject
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveNamedAddress
import org.move.lang.core.resolve.ref.NamedAddressReference
import org.move.openapiext.parentTable
import org.move.openapiext.stringValue
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTableHeader

val TomlTableHeader.isAddressesHeader: Boolean get() = text in setOf("[addresses]")

val MoveNamedAddress.addressTomlKeyValue: TomlKeyValue? get() {
    val segment = this.reference?.resolve() ?: return null
    return segment.ancestorStrict()
}

//fun MoveNamedAddress.addressValue(): String? {
//    val moveProject = this.containingFile.containingMoveProject() ?: return null
//    moveProject.getAddressValue()
//}
//val MoveNamedAddress.addressValue: String? get() {
//    return this.addressTomlKeyValue?.value?.stringValue()
//}

fun TomlKeySegment.isNamedAddressDef(): Boolean {
    val key = this.parent as? TomlKey ?: return false
    val tomlTable = (key.parent as? TomlKeyValue)?.parentTable ?: return false
    return tomlTable.header.isAddressesHeader
}

abstract class MoveNamedAddressMixin(node: ASTNode) : MoveElementImpl(node),
                                                      MoveNamedAddress {
    override fun getReferences(): Array<PsiReference> {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }

    override fun getReference(): NamedAddressReference = references.last() as NamedAddressReference
}
