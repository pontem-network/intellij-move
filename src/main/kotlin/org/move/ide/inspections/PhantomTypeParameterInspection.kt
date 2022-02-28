package org.move.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.declaredTy
import org.move.lang.core.psi.ext.fields
import org.move.lang.core.psi.ext.isPhantom
import org.move.lang.core.types.infer.foldTyTypeParameterWith

class PhantomTypeParameterInspection: MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        return object : MvVisitor() {
            override fun visitTypeParameterList(o: MvTypeParameterList) {
                val struct = o.ancestorStrict<MvStruct>(stopAt = MvCodeBlock::class.java) ?: return
                val fields = struct.fields
                val usedParamNames = mutableListOf<String>()
                for (field in fields) {
                    field.declaredTy.foldTyTypeParameterWith { param ->
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
                            object: LocalQuickFix {
                                override fun getFamilyName() = "Declare phantom"

                                override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                                    val newParam = project.psiFactory.createTypeParameter("phantom $name")
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
