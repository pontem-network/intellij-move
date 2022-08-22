package org.move.ide.inspections

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.move.cli.Consts
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.toAddress
import org.move.lang.core.types.Address

class AddressByValueImportInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor =
        object : MvVisitor() {
            override fun visitModuleUseSpeck(o: MvModuleUseSpeck) {
                val moduleRef = o.fqModuleRef ?: return
                // no error if unresolved by value (default)
                val module = moduleRef.reference?.resolve() as? MvModule ?: return

                val refAddress = moduleRef.addressRef.toAddress() ?: return
                if (refAddress.value == Consts.ADDR_PLACEHOLDER) return
                if (refAddress !is Address.Named) return

                val modAddress = module.addressRef?.toAddress() ?: return
                if (modAddress.value == Consts.ADDR_PLACEHOLDER) return
                if (modAddress !is Address.Named) return

                if (refAddress != modAddress) {
                    holder.registerProblem(
                        moduleRef,
                        "Module is declared with a different address `${modAddress.name}`",
                        ProblemHighlightType.WEAK_WARNING,
                        object : InspectionQuickFix("Change address to `${modAddress.name}`") {
                            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                                val ref = descriptor.psiElement as MvFQModuleRef
                                // resolve by value
                                val mod = ref.reference?.resolve() as? MvModule ?: return
                                val modAddressRef = mod.addressRef ?: return
                                if (ref.addressRef.toAddress() != mod.addressRef?.toAddress()) {
                                    val newAddressRef = project.psiFactory.addressRef(modAddressRef.text)
                                    ref.addressRef.replace(newAddressRef)
                                }
                            }

                        }
                    )
                }
            }
        }
}
