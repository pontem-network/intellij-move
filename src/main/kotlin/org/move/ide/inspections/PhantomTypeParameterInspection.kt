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
                    val paramNames = mutableListOf<String>()
                    field.declaredTy(false).foldTyTypeParameterWith { param ->
                        val typeParameter = param.parameter
                        val name = typeParameter.name
                        if (name != null) {
                            paramNames.add(name)
                        }
                        param
                    }
                    run {
                        val fieldType = field.typeAnnotation?.type ?: return@run
                        val fieldStruct = fieldType.moveReference?.resolve() as? MvStruct ?: return@run
                        for ((i, typeArg) in fieldType.typeArguments.withIndex()) {
                            val typeParam = typeArg.type
                                .moveReference?.resolve() as? MvTypeParameter ?: continue
                            val structTypeParam = fieldStruct.typeParameters.getOrNull(i)
                            if (structTypeParam != null && structTypeParam.isPhantom) {
                                paramNames.remove(typeParam.name)
                            }
                        }
                    }
                    usedParamNames.addAll(paramNames)
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
                                    val tp = descriptor.psiElement as? MvTypeParameter ?: return
                                    if (tp.abilities.isNotEmpty()) {
                                        paramText +=
                                            ": " + tp.abilities.joinToString(", ") { it.text }
                                    }
                                    val newParam = project.psiFactory.typeParameter(paramText)
                                    tp.replace(newParam)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
