package org.move.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.util.descendantsOfType
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
                        val typeLists = fieldType.descendantsOfType<MvTypeArgumentList>()
                        for (typeList in typeLists) {
                            val path = typeList.parent as MvPath
                            val outerStruct = path.reference?.resolve() as? MvStruct ?: continue
                            for ((i, typeArg) in typeList.typeArgumentList.withIndex()) {
                                val typeParam = typeArg.type
                                    .moveReference?.resolve() as? MvTypeParameter ?: continue
                                val outerTypeParam = outerStruct.typeParameters.getOrNull(i) ?: continue
                                if (outerTypeParam.isPhantom) {
                                    paramNames.remove(typeParam.name)
                                }
                            }
                        }
                    }
                    usedParamNames.addAll(paramNames)
                }
                for (typeParam in o.typeParameterList) {
                    val typeParamName = typeParam.name ?: continue
                    if (typeParam.isPhantom) {
                        if (typeParamName !in usedParamNames) {
                            continue
                        } else {
                            holder.registerProblem(
                                typeParam,
                                "Cannot be phantom",
                                object : LocalQuickFix {
                                    override fun getFamilyName(): String {
                                        return "Remove phantom"
                                    }

                                    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                                        var paramText = typeParamName
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
                    if (typeParamName !in usedParamNames) {
                        holder.registerProblem(
                            typeParam,
                            "Unused type parameter. Consider declaring it as phantom",
                            object : LocalQuickFix {
                                override fun getFamilyName() = "Declare phantom"

                                override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                                    var paramText = "phantom $typeParamName"
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
