package org.move.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.infer.foldTyTypeParameterWith

class PhantomTypeParameterInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        return object : MvVisitor() {
            override fun visitTypeParameterList(o: MvTypeParameterList) {
                val struct = o.ancestorStrict<MvStruct>(stopAt = MvCodeBlock::class.java) ?: return
                val fields = struct.fields
                val usedParamNames = mutableListOf<String>()
                for (field in fields) {
                    field.declaredTy(false).foldTyTypeParameterWith { param ->
                        val name = param.parameter.name
                        if (name != null) {
                            usedParamNames.add(name)
                        }
                        param
                    }
                }
                for (typeParam in o.typeParameterList) {
                    if (typeParam.isPhantom) continue
                    val name = typeParam.name ?: continue
                    if (name !in usedParamNames) {
                        holder.registerProblem(
                            typeParam,
                            "Unused type parameter. Consider declaring it as phantom",
                            object : LocalQuickFix {
                                override fun getFamilyName() = "Declare phantom"

                                override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                                    var paramText = "phantom $name"
                                    if (typeParam.abilities.isNotEmpty()) {
                                        paramText += ": " + typeParam.abilities.joinToString(", ") { it.text }
                                    }
                                    val newParam = project.psiFactory.typeParameter(paramText)
                                    typeParam.replace(newParam)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
