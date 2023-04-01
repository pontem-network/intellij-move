package org.move.ide.inspections

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.move.cli.Consts
import org.move.lang.core.psi.*
import org.move.lang.core.types.Address
import org.move.lang.core.types.address
import org.move.lang.moveProject

class AddressByValueImportInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor =
        object : MvVisitor() {
            override fun visitModuleUseSpeck(o: MvModuleUseSpeck) {
                val moduleRef = o.fqModuleRef ?: return
                // no error if unresolved by value (default)
                val module = moduleRef.reference?.resolve() as? MvModule ?: return

                val moveProj = moduleRef.moveProject ?: return

                val refAddress = moduleRef.addressRef.address(moveProj) ?: return
                if (refAddress !is Address.Named) return
                if (refAddress.value == Consts.ADDR_PLACEHOLDER) return

                val modAddress = module.address(moveProj) ?: return
                if (modAddress !is Address.Named) return
                if (modAddress.value == Consts.ADDR_PLACEHOLDER) return

                if (!Address.eq(refAddress, modAddress)) {
                    holder.registerProblem(
                        moduleRef,
                        "Module is declared with a different address `${modAddress.name}`",
                        ProblemHighlightType.WEAK_WARNING,
                        object : InspectionQuickFix("Change address to `${modAddress.name}`") {
                            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                                val ref = descriptor.psiElement as MvFQModuleRef

                                // resolve by value
                                val mod = ref.reference?.resolve() as? MvModule ?: return
                                val proj = mod.moveProject ?: return

                                val modAddressRef = mod.addressRef ?: return
                                if (ref.addressRef.address(proj) != mod.address(proj)) {
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
