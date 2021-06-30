package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.R_PAREN
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.HasType
import org.move.lang.core.types.StructType
import org.move.lang.core.types.TypeParamType

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

            override fun visitModuleDef(o: MoveModuleDef) = checkModuleDef(moveHolder, o)

            override fun visitStructFieldDef(o: MoveStructFieldDef) = checkStructFieldDef(moveHolder, o)

            override fun visitCallExpr(o: MoveCallExpr) {
                if (o.referenceName !in ACQUIRES_BUILTIN_FUNCTIONS) return

                val paramType =
                    o.typeArguments.getOrNull(0)
                        ?.type?.resolvedType(emptyMap()) as? StructType ?: return
                val paramTypeName = paramType.name()

                val containingFunction = o.containingFunction ?: return
                val signature = containingFunction.functionSignature ?: return

                val name = signature.name ?: return
                val errorMessage = "Function '$name' is not marked as 'acquires $paramTypeName'"
                val acquiresType = signature.acquiresType
                if (acquiresType == null) {
                    moveHolder.createErrorAnnotation(o, errorMessage)
                    return
                }
                val acquiresTypeNames = acquiresType.typeNames ?: return
                if (paramTypeName !in acquiresTypeNames) {
                    moveHolder.createErrorAnnotation(o, errorMessage)
                }
            }

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

                for (field in o.structLiteralFieldsBlock.structLiteralFieldList) {
                    val assignmentExpr = field.structLiteralFieldAssignment?.expr ?: continue
                    val assignmentType = assignmentExpr.resolvedType(emptyMap())
                    if (assignmentType == null) continue

                    val fieldName = field.referenceName ?: continue
                    val fieldDef = referredStructDef.getField(fieldName) ?: continue
                    val expectedFieldType = fieldDef.resolvedType(emptyMap())
                    if (expectedFieldType is TypeParamType) {
                        checkHasRequiredAbilities(moveHolder, assignmentExpr, expectedFieldType)
                        return
                    }
                    val exprType = assignmentExpr.resolvedType(emptyMap())
                    if (expectedFieldType != null
                        && exprType != null
                        && !expectedFieldType.compatibleWith(exprType)
                    ) {
                        val exprTypeName = exprType.typeLabel(relativeTo = o)
                        val expectedTypeName = expectedFieldType.typeLabel(relativeTo = o)

                        val message =
                            "Invalid argument for field '$fieldName': " +
                                    "type '$exprTypeName' is not compatible with '$expectedTypeName'"
                        moveHolder.createErrorAnnotation(assignmentExpr, message)
                    }
                }
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

        val signature = structField.structDef?.structSignature ?: return
        val structType = StructType(signature)
        val structAbilities = structType.abilities()
        if (structAbilities.isEmpty()) return

        val fieldType = structField.typeAnnotation?.type?.resolvedType(emptyMap()) as? StructType ?: return

        for (ability in structAbilities) {
            val requiredAbility = ability.requires()
            if (requiredAbility !in fieldType.abilities()) {
                val message =
                    "The type '${fieldType.name()}' does not have the ability '${requiredAbility.label()}' " +
                            "required by the declared ability '${ability.label()}' " +
                            "of the struct '${structType.name()}'"
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
    val callExpr = arguments.parent as? MoveCallExpr ?: return
    val signature = callExpr.reference?.resolve() as? MoveFunctionSignature ?: return

    val expectedCount = signature.parameters.size
    val realCount = arguments.exprList.size
    val errorMessage =
        "This function takes $expectedCount ${pluralise(expectedCount, "parameter", "parameters")} " +
                "but $realCount ${pluralise(realCount, "parameter", "parameters")} " +
                "${pluralise(realCount, "was", "were")} supplied"
    when {
        realCount < expectedCount -> {
            val target = arguments.findFirstChildByType(R_PAREN) ?: arguments
            holder.createErrorAnnotation(target, errorMessage)
            return
        }
        realCount > expectedCount -> {
            arguments.exprList.drop(expectedCount).forEach {
                holder.createErrorAnnotation(it, errorMessage)
            }
            return
        }
    }

    val callExprTypeVars = callExpr.typeVars
    for ((i, expr) in arguments.exprList.withIndex()) {
        val parameter = signature.parameters[i]
        val expectedType = parameter.resolvedType(callExprTypeVars)
        if (expectedType is TypeParamType) {
            checkHasRequiredAbilities(holder, expr, expectedType)
            return
        }

        val exprType = expr.resolvedType(emptyMap())
        if (expectedType != null && exprType != null
            && !expectedType.compatibleWith(exprType)
        ) {
            val paramName = parameter.name ?: continue
            val exprTypeName = exprType.typeLabel(relativeTo = arguments)
            val expectedTypeName = expectedType.typeLabel(relativeTo = arguments)

            val message =
                "Invalid argument for parameter '$paramName': " +
                        "type '$exprTypeName' is not compatible with '$expectedTypeName'"
            holder.createErrorAnnotation(expr, message)
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
                val typeParam = referred.typeParameters[i]
                val typeParamType = typeParam.resolvedType(emptyMap()) as? TypeParamType ?: continue
                checkHasRequiredAbilities(
                    holder,
                    typeArgument.type,
                    typeParamType
                )
            }
        }
    }
}

private fun checkHasRequiredAbilities(
    holder: MoveAnnotationHolder,
    element: HasType, typeParamType: TypeParamType
) {
    val elementType = element.resolvedType(emptyMap()) ?: return
    // do not check for specs
    if (element.ancestorStrict<MoveSpecDef>() != null) return

    val abilities = elementType.abilities()
    for (ability in typeParamType.abilities()) {
        if (ability !in abilities) {
            val typeName = elementType.typeLabel(relativeTo = element)
            holder.createErrorAnnotation(
                element,
                "The type '$typeName' " +
                        "does not have required ability '${ability.label()}'"
            )
            return
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
