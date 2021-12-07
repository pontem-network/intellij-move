package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvNamedAddress
import org.move.lang.core.resolve.ref.NamedAddressReference
import org.move.openapiext.parentTable
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTableHeader

val TomlTableHeader.isAddressesHeader: Boolean get() = text in setOf("[addresses]")

val MvNamedAddress.addressTomlKeyValue: TomlKeyValue? get() {
    val segment = this.reference.resolve() ?: return null
    return segment.ancestorStrict()
}

//fun MvNamedAddress.addressValue(): String? {
//    val moveProject = this.containingFile.containingMvProject() ?: return null
//    moveProject.getAddressValue()
//}
//val MvNamedAddress.addressValue: String? get() {
//    return this.addressTomlKeyValue?.value?.stringValue()
//}

fun TomlKeySegment.isNamedAddressDef(): Boolean {
    val key = this.parent as? TomlKey ?: return false
    val tomlTable = (key.parent as? TomlKeyValue)?.parentTable ?: return false
    return tomlTable.header.isAddressesHeader
}

abstract class MvNamedAddressMixin(node: ASTNode) : MvElementImpl(node),
                                                      MvNamedAddress {
    override fun getReferences(): Array<PsiReference> {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }

    override fun getReference(): NamedAddressReference = references.last() as NamedAddressReference
}
