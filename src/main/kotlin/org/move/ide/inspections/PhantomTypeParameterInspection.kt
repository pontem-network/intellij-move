package org.move.ide.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.descendantsOfType
import org.move.ide.inspections.fixes.PhantomFix
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.fields
import org.move.lang.core.psi.ext.isPhantom
import org.move.lang.core.psi.ext.moveReference
import org.move.lang.core.psi.ext.typeArguments

class PhantomTypeParameterInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        return object : MvVisitor() {
            override fun visitStruct(o: MvStruct) {
                val usedTypeParams = mutableSetOf<MvTypeParameter>()

                for (structField in o.fields) {
                    val fieldUsedTypeParams = mutableSetOf<MvTypeParameter>()

                    val fieldType = structField.typeAnnotation?.type ?: continue
                    for (path in fieldType.descendantsOfType<MvPath>()) {
                        if (path.typeArguments.isNotEmpty()) continue
                        val typeParam = path.reference?.resolve() as? MvTypeParameter ?: continue
                        fieldUsedTypeParams.add(typeParam)
                    }

                    // find all MvTypeArgument, check their phantom status
                    val paths = fieldType.descendantsOfType<MvPath>()
                    for (path in paths) {
                        // stop if empty
                        if (path.typeArguments.isEmpty()) continue
                        // determine phantom status of every argument, drop if phantom
                        val outerStruct = path.reference?.resolveWithAliases() as? MvStruct ?: continue
                        for ((i, typeArg) in path.typeArguments.withIndex()) {
                            val outerTypeParam = outerStruct.typeParameters.getOrNull(i) ?: continue
                            if (outerTypeParam.isPhantom) {
                                val typeParam =
                                    typeArg.type.moveReference?.resolve() as? MvTypeParameter ?: continue
                                fieldUsedTypeParams.remove(typeParam)
                            }
                        }
                    }
                    usedTypeParams.addAll(fieldUsedTypeParams)
                }

                for (typeParam in o.typeParameters) {
                    when {
                        typeParam.isPhantom && typeParam in usedTypeParams -> {
                            holder.registerProblem(
                                typeParam,
                                "Cannot be phantom",
                                PhantomFix.Remove(typeParam)
                            )
                        }
                        !typeParam.isPhantom && typeParam !in usedTypeParams -> {
                            holder.registerProblem(
                                typeParam,
                                "Unused type parameter. Consider declaring it as phantom",
                                PhantomFix.Add(typeParam)
                            )

                        }
                    }
                }
            }
        }
    }
}
