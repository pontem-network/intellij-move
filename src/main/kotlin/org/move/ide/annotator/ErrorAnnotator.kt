package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.R_PAREN
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.StructType

class ErrorAnnotator : MoveAnnotator() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val moveHolder = MoveAnnotationHolder(holder)
        val visitor = object : MoveVisitor() {
            override fun visitConstDef(o: MoveConstDef) = checkConstDef(moveHolder, o)

            override fun visitFunctionSignature(o: MoveFunctionSignature) =
                checkFunctionSignature(moveHolder, o)

            override fun visitStructSignature(o: MoveStructSignature) {
                checkStructSignature(moveHolder, o)
            }
//            override fun visitFunctionDef(o: MoveFunctionDef) = checkFunctionDef(moveHolder, o)
//            override fun visitNativeFunctionDef(o: MoveNativeFunctionDef) =
//                checkNativeFunctionDef(moveHolder, o)

            override fun visitModuleDef(o: MoveModuleDef) = checkModuleDef(moveHolder, o)

            //            override fun visitStructDef(o: MoveStructDef) = checkStructDef(moveHolder, o)
//            override fun visitNativeStructDef(o: MoveNativeStructDef) = checkNativeStructDef(moveHolder, o)
            override fun visitStructFieldDef(o: MoveStructFieldDef) = checkStructFieldDef(moveHolder, o)

//            override fun visitQualPath(o: MoveQualPath) = checkTypeArguments(moveHolder, o)

            override fun visitCallArguments(o: MoveCallArguments) = checkCallArguments(moveHolder, o)

            override fun visitStructPat(o: MoveStructPat) {
                val fieldNames = o.providedFields.mapNotNull { it.referenceName }
                val referredStructDef = o.referredStructDef ?: return
                val nameElement = o.referenceNameElement ?: return
                checkMissingFields(moveHolder, nameElement, fieldNames.toSet(), referredStructDef)
            }

            override fun visitStructLiteralExpr(o: MoveStructLiteralExpr) {
                val referredStructDef = o.referredStructDef ?: return
                val nameElement = o.referenceNameElement ?: return
                checkMissingFields(
                    moveHolder,
                    nameElement,
                    o.providedFieldNames.toSet(),
                    referredStructDef
                )
            }

            override fun visitQualPath(o: MoveQualPath) = checkQualPath(moveHolder, o)
        }
        element.accept(visitor)
    }

    private fun checkStructSignature(holder: MoveAnnotationHolder, signature: MoveStructSignature) {
        checkStructSignatureDuplicates(holder, signature)
    }

    private fun checkFunctionSignature(holder: MoveAnnotationHolder, signature: MoveFunctionSignature) {
        checkFunctionSignatureDuplicates(holder, signature)
        warnOnBuiltInFunctionName(holder, signature)
    }

    private fun checkModuleDef(holder: MoveAnnotationHolder, mod: MoveModuleDef) {
        checkDuplicates(holder, mod)
    }

    private fun checkStructFieldDef(holder: MoveAnnotationHolder, structField: MoveStructFieldDef) {
        checkDuplicates(holder, structField)

        val parentStructType = structField.structDef?.structType ?: return
        val structAbilities = parentStructType.abilities()
        if (structAbilities.isEmpty()) return

        val fieldType = structField.typeAnnotation?.type?.resolvedType as? StructType ?: return

        for (ability in structAbilities) {
            val requiredAbility = ability.requires()
            if (requiredAbility !in fieldType.abilities()) {
                val message =
                    "The type '${fieldType.name()}' does not have the ability '${requiredAbility.label()}' " +
                            "required by the declared ability '${ability.label()}' " +
                            "of the struct '${parentStructType.name()}'"
                holder.createErrorAnnotation(structField, message)
                return
            }
        }
    }

    private fun checkConstDef(holder: MoveAnnotationHolder, const: MoveConstDef) {
        checkDuplicates(holder, const)
    }
}

private fun checkMissingFields(
    holder: MoveAnnotationHolder,
    target: PsiElement,
    providedFieldNames: Set<String>,
    referredStruct: MoveStructDef,
) {
    if ((referredStruct.fieldNames.toSet() - providedFieldNames).isNotEmpty()) {
        holder.createErrorAnnotation(target, "Some fields are missing")
    }
}

private fun checkCallArguments(holder: MoveAnnotationHolder, arguments: MoveCallArguments) {
    val expectedCount = (arguments.parent as? MoveCallExpr)?.expectedParamsCount() ?: return
    val realCount = arguments.exprList.size
    val errorMessage =
        "This function takes $expectedCount ${pluralise(expectedCount, "parameter", "parameters")} " +
                "but $realCount ${pluralise(realCount, "parameter", "parameters")} " +
                "${pluralise(realCount, "was", "were")} supplied"
    when {
        realCount < expectedCount -> {
            val target = arguments.findFirstChildByType(R_PAREN) ?: arguments
            holder.createErrorAnnotation(target, errorMessage)
        }
        realCount > expectedCount -> {
            arguments.exprList.drop(expectedCount).forEach {
                holder.createErrorAnnotation(it, errorMessage)
            }
        }
    }
}

private fun checkQualPath(holder: MoveAnnotationHolder, qualPath: MoveQualPath) {
    val typeArguments = qualPath.typeArguments
    val referred =
        (qualPath.parent as? MoveQualPathReferenceElement)?.reference?.resolve()

    if (referred == null) {
        if (qualPath.identifierName == "vector") {
            if (typeArguments.isEmpty()) {
                holder.createErrorAnnotation(qualPath.identifier, "Missing item type argument")
                return
            }
            val realCount = typeArguments.size
            if (realCount > 1) {
                typeArguments.drop(1).forEach {
                    holder.createErrorAnnotation(
                        it,
                        "Wrong number of type arguments: expected 1, found $realCount"
                    )
                }
                return
            }
        }
        return
    }

    val name = referred.name ?: return
    when {
        referred is MoveFunctionSignature
                && name in BUILTIN_FUNCTIONS_WITH_REQUIRED_RESOURCE_TYPE
                && typeArguments.isEmpty() -> {
            holder.createErrorAnnotation(qualPath, "Missing resource type argument")
            return
        }
        referred is MoveTypeParametersOwner -> {
            val expectedCount = referred.typeParameters.size
            val realCount = typeArguments.size

            if (expectedCount == 0 && realCount != 0) {
                holder.createErrorAnnotation(
                    qualPath.typeArgumentList!!,
                    "No type arguments expected"
                )
                return
            }
            if (realCount > expectedCount) {
                typeArguments.drop(expectedCount).forEach {
                    holder.createErrorAnnotation(
                        it,
                        "Wrong number of type arguments: expected $expectedCount, found $realCount"
                    )
                }
                return
            }

            for ((i, typeArgument) in typeArguments.withIndex()) {
                val resolvedType = typeArgument.type.resolvedType

                val requiredAbilities = referred.typeParameters[i].typeParamType.abilities()
                val abilities = resolvedType.abilities()

                for (ability in requiredAbilities) {
                    if (ability !in abilities) {
                        holder.createErrorAnnotation(
                            typeArgument,
                            "The type '${resolvedType.fullname()}' " +
                                    "does not have required ability '${ability.label()}'"
                        )
                    }
                }
                return
            }
        }
    }
}

private fun checkDuplicates(
    holder: MoveAnnotationHolder,
    element: MoveNameIdentifierOwner,
    scope: PsiElement = element.parent,
) {
    val duplicateNamedChildren = getDuplicatedNamedChildren(scope)
    if (element.name !in duplicateNamedChildren.map { it.name }) {
        return
    }
    val identifier = element.nameIdentifier ?: element
    holder.createErrorAnnotation(identifier, "Duplicate definitions with name `${element.name}`")
}

private fun checkFunctionSignatureDuplicates(
    holder: MoveAnnotationHolder,
    fnSignature: MoveFunctionSignature,
) {
    val fnSignatures =
        fnSignature.module?.allFnSignatures()
            ?: fnSignature.script?.allFnSignatures()
            ?: emptyList()
    val duplicateSignatures = getDuplicates(fnSignatures.asSequence())

    if (fnSignature.name !in duplicateSignatures.map { it.name }) {
        return
    }
    val identifier = fnSignature.nameIdentifier ?: fnSignature
    holder.createErrorAnnotation(identifier, "Duplicate definitions with name `${fnSignature.name}`")
}

private fun checkStructSignatureDuplicates(
    holder: MoveAnnotationHolder,
    structSignature: MoveStructSignature,
) {
    val duplicateSignatures = getDuplicates(structSignature.module.structSignatures().asSequence())
    if (structSignature.name !in duplicateSignatures.map { it.name }) {
        return
    }
    val identifier = structSignature.nameIdentifier ?: structSignature
    holder.createErrorAnnotation(identifier, "Duplicate definitions with name `${structSignature.name}`")
}

private fun getDuplicates(elements: Sequence<MoveNamedElement>): Set<MoveNamedElement> {
    return elements
        .groupBy { it.name }
        .map { it.value }
        .filter { it.size > 1 }
        .flatten()
        .toSet()
}

private fun getDuplicatedNamedChildren(owner: PsiElement): Set<MoveNamedElement> {
    return owner
        .namedChildren()
        .groupBy { it.name }
        .map { it.value }
        .filter { it.size > 1 }
        .flatten()
        .toSet()
}

private fun PsiElement.namedChildren(): Sequence<MoveNamedElement> {
    return this.children.filterIsInstance<MoveNamedElement>().asSequence()
}

private fun warnOnBuiltInFunctionName(holder: MoveAnnotationHolder, element: MoveNamedElement) {
    val nameElement = element.nameElement ?: return
    val name = element.name ?: return
    if (name in BUILTIN_FUNCTIONS) {
        holder.createErrorAnnotation(nameElement, "Invalid function name: `$name` is a built-in function")
    }
}
