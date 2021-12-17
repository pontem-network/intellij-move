package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import org.move.ide.presentation.name
import org.move.ide.presentation.typeLabel
import org.move.lang.MvElementTypes.R_PAREN
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.mixins.declaredTy
import org.move.lang.core.psi.mixins.ty
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.isCompatible
import org.move.lang.core.types.ty.*

class ErrorAnnotator : MvAnnotator() {
    companion object {
        private fun invalidReturnTypeMessage(expectedType: Ty, actualType: Ty): String {
            return "Invalid return type: " +
                    "expected '${expectedType.name()}', found '${actualType.name()}'"
        }
    }

    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val moveHolder = MvAnnotationHolder(holder)
        val visitor = object : MvVisitor() {
            override fun visitConstDef(o: MvConstDef) = checkConstDef(moveHolder, o)

            override fun visitIfExpr(o: MvIfExpr) {
                val ifTy = o.returningExpr?.inferExprTy() ?: return
                val elseExpr = o.elseExpr ?: return
                val elseTy = elseExpr.inferExprTy()

                if (!isCompatible(ifTy, elseTy)) {
                    moveHolder.createErrorAnnotation(
                        elseExpr,
                        "Incompatible type '${elseTy.typeLabel(o)}'" +
                                ", expected '${ifTy.typeLabel(o)}'"
                    )
                }
            }

            override fun visitCondition(o: MvCondition) {
                val expr = o.expr ?: return
                val exprTy = expr.inferExprTy()
                if (!isCompatible(exprTy, TyBool)) {
                    moveHolder.createErrorAnnotation(
                        expr,
                        "Incompatible type '${exprTy.typeLabel(o)}', expected 'bool'"
                    )
                }
            }

            override fun visitFunction(o: MvFunction) {
                checkFunction(moveHolder, o)
            }

            override fun visitStruct_(o: MvStruct_) {
                checkStruct(moveHolder, o)
            }

            override fun visitModuleDef(o: MvModuleDef) = checkModuleDef(moveHolder, o)

            override fun visitStructFieldDef(o: MvStructFieldDef) = checkStructFieldDef(moveHolder, o)

            override fun visitCodeBlock(codeBlock: MvCodeBlock) {
                val fn = codeBlock.parent as? MvFunction ?: return
                val returningExpr = codeBlock.returningExpr

                val expectedReturnTy = fn.resolvedReturnTy
                val actualReturnTy = returningExpr?.inferExprTy() ?: TyUnit
                if (!isCompatible(expectedReturnTy, actualReturnTy)) {
                    val annotatedElement = returningExpr as? PsiElement
                        ?: codeBlock.rightBrace
                        ?: codeBlock
                    moveHolder.createErrorAnnotation(
                        annotatedElement,
                        invalidReturnTypeMessage(expectedReturnTy, actualReturnTy)
                    )
                }
            }

            override fun visitReturnExpr(o: MvReturnExpr) {
                val outerFn = o.containingFunction ?: return
                val expectedReturnTy = outerFn.resolvedReturnTy
                val actualReturnTy = o.expr?.inferExprTy() ?: return
                if (!isCompatible(expectedReturnTy, actualReturnTy)) {
                    moveHolder.createErrorAnnotation(
                        o,
                        invalidReturnTypeMessage(expectedReturnTy, actualReturnTy)
                    )
                }
            }

            override fun visitCallExpr(o: MvCallExpr) {
                if (o.path.referenceName !in ACQUIRES_BUILTIN_FUNCTIONS) return

                val paramType =
                    o.typeArguments.getOrNull(0)
                        ?.type?.inferTypeTy() as? TyStruct ?: return
                val paramTypeFQName = paramType.item.fqName
                val paramTypeName = paramType.item.name ?: return

                val containingFunction = o.containingFunction ?: return

                val name = containingFunction.name ?: return
                val errorMessage = "Function '$name' is not marked as 'acquires $paramTypeName'"
                val acquiresType = containingFunction.acquiresType
                if (acquiresType == null) {
                    moveHolder.createErrorAnnotation(o, errorMessage)
                    return
                }
                val acquiresTypeNames = acquiresType.typeFQNames ?: return
                if (paramTypeFQName !in acquiresTypeNames) {
                    moveHolder.createErrorAnnotation(o, errorMessage)
                }
            }

            override fun visitCallArgumentList(o: MvCallArgumentList) = checkCallArgumentList(moveHolder, o)

            override fun visitStructPat(o: MvStructPat) {
                val nameElement = o.path.referenceNameElement ?: return
                val refStruct = o.path.maybeStruct ?: return
                val fieldNames = o.fields.map { it.referenceName }
                checkMissingFields(
                    moveHolder, nameElement, fieldNames.toSet(), refStruct
                )
            }

            override fun visitStructLitExpr(o: MvStructLitExpr) {
                val nameElement = o.path.referenceNameElement ?: return
                val refStruct = o.path.maybeStruct ?: return
                checkMissingFields(
                    moveHolder, nameElement, o.providedFieldNames.toSet(), refStruct
                )

                val ctx = InferenceContext()
                for (field in o.structLitFieldsBlock.structLitFieldList) {
                    val assignmentExpr = field.structLitFieldAssignment?.expr ?: continue
                    val assignmentType = assignmentExpr.inferExprTy(ctx)
                    if (assignmentType is TyUnknown) continue

                    val fieldName = field.referenceName
                    val fieldDef = refStruct.getField(fieldName) ?: continue
                    val expectedFieldTy = fieldDef.declaredTy
                    val exprTy = assignmentExpr.inferExprTy(ctx)

                    if (!isCompatible(expectedFieldTy, exprTy)) {
                        val exprTypeName = exprTy.typeLabel(relativeTo = o)
                        val expectedTypeName = expectedFieldTy.typeLabel(relativeTo = o)

                        val message =
                            "Invalid argument for field '$fieldName': " +
                                    "type '$exprTypeName' is not compatible with '$expectedTypeName'"
                        moveHolder.createErrorAnnotation(assignmentExpr, message)
                    }
                }
            }

            override fun visitPath(o: MvPath) = checkPath(moveHolder, o)
//            override fun visitPathWithTypeArgs(o: MvPathWithTypeArgs) {
//                checkPath(moveHolder, o)
//            }
//            override fun visitQualPath(o: MvPathOptTypeArguments) = checkQualPath(moveHolder, o)
        }
        element.accept(visitor)
    }

    private fun checkStruct(holder: MvAnnotationHolder, struct: MvStruct_) {
        checkStructDuplicates(holder, struct)
    }

    private fun checkFunction(holder: MvAnnotationHolder, function: MvFunction) {
        checkFunctionSignatureDuplicates(holder, function)
        warnOnBuiltInFunctionName(holder, function)
    }

    private fun checkModuleDef(holder: MvAnnotationHolder, mod: MvModuleDef) {
        val modIdent = Pair(mod.definedAddressRef()?.toAddress(), mod.name)
        val file = mod.containingFile ?: return
        val duplicateIdents =
            file.descendantsOfType<MvModuleDef>()
                .filter { it.name != null }
                .groupBy { Pair(it.definedAddressRef()?.toAddress(), it.name) }
                .filter { it.value.size > 1 }
                .map { it.key }
                .toSet()
        if (modIdent !in duplicateIdents) return

        val identifier = mod.nameIdentifier ?: mod
        holder.createErrorAnnotation(identifier, "Duplicate definitions with name `${mod.name}`")
    }

    private fun checkStructFieldDef(holder: MvAnnotationHolder, structField: MvStructFieldDef) {
        checkDuplicates(holder, structField)

        val structTy = TyStruct(structField.struct)
        val structAbilities = structTy.abilities()
        if (structAbilities.isEmpty()) return

        val fieldTy = structField.declaredTy as? TyStruct ?: return
        for (ability in structAbilities) {
            val requiredAbility = ability.requires()
            if (requiredAbility !in fieldTy.abilities()) {
                val message =
                    "The type '${fieldTy.name()}' does not have the ability '${requiredAbility.label()}' " +
                            "required by the declared ability '${ability.label()}' " +
                            "of the struct '${structTy.name()}'"
                holder.createErrorAnnotation(structField, message)
                return
            }
        }
    }

    private fun checkConstDef(holder: MvAnnotationHolder, const: MvConstDef) {
        val binding = const.bindingPat ?: return
        val owner = const.parent?.parent ?: return
        val allBindings = when (owner) {
            is MvModuleDef -> owner.constBindings()
            is MvScriptDef -> owner.constBindings()
            else -> return
        }
        checkDuplicates(holder, binding, allBindings.asSequence())
    }
}

private fun checkMissingFields(
    holder: MvAnnotationHolder,
    target: PsiElement,
    providedFieldNames: Set<String>,
    referredStruct: MvStruct_,
) {
    if ((referredStruct.fieldNames.toSet() - providedFieldNames).isNotEmpty()) {
        holder.createErrorAnnotation(target, "Some fields are missing")
    }
}

private fun checkCallArgumentList(holder: MvAnnotationHolder, arguments: MvCallArgumentList) {
    val callExpr = arguments.parent as? MvCallExpr ?: return
    val function = callExpr.path.reference?.resolve() as? MvFunction ?: return

    val expectedCount = function.parameters.size
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

    val ctx = InferenceContext()
    for ((i, expr) in arguments.exprList.withIndex()) {
        val parameter = function.parameters[i]
        val expectedTy = parameter.declaredTy
        val exprTy = expr.inferExprTy(ctx)
        if (expectedTy is TyTypeParameter) {
            checkHasRequiredAbilities(holder, expr, exprTy, expectedTy)
            return
        }
        if (!isCompatible(expectedTy, exprTy)) {
            val paramName = parameter.bindingPat.name ?: continue
            val exprTypeName = exprTy.typeLabel(relativeTo = arguments)
            val expectedTypeName = expectedTy.typeLabel(relativeTo = arguments)

            val message =
                "Invalid argument for parameter '$paramName': " +
                        "type '$exprTypeName' is not compatible with '$expectedTypeName'"
            holder.createErrorAnnotation(expr, message)
        }
    }
}

private fun checkPath(holder: MvAnnotationHolder, path: MvPath) {
    val identifier = path.identifier ?: return

    val typeArguments = path.typeArguments
    val referred = path.reference?.resolve()

    if (referred == null) {
        if (path.identifierName == "vector") {
            if (typeArguments.isEmpty()) {
                holder.createErrorAnnotation(identifier, "Missing item type argument")
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
        referred is MvFunction
                && name in BUILTIN_FUNCTIONS_WITH_REQUIRED_RESOURCE_TYPE
                && typeArguments.isEmpty() -> {
            holder.createErrorAnnotation(path, "Missing resource type argument")
            return
        }
        referred is MvTypeParametersOwner -> {
            val expectedCount = referred.typeParameters.size
            val realCount = typeArguments.size

            if (expectedCount == 0 && realCount != 0) {
                holder.createErrorAnnotation(
                    path.typeArgumentList!!,
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
                val argumentTy = typeArgument.type.inferTypeTy()
                checkHasRequiredAbilities(
                    holder,
                    typeArgument.type,
                    argumentTy,
                    typeParam.ty()
                )
            }
        }
    }
}

private fun checkHasRequiredAbilities(
    holder: MvAnnotationHolder,
    element: MvElement,
    elementTy: Ty,
    typeParamType: TyTypeParameter
) {
    // do not check for specs
    if (element.isInsideSpecBlock()) return

    val abilities = elementTy.abilities()
    for (ability in typeParamType.abilities()) {
        if (ability !in abilities) {
            val typeName = elementTy.typeLabel(relativeTo = element)
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
    holder: MvAnnotationHolder,
    element: MvNameIdentifierOwner,
    scopeNamedChildren: Sequence<MvNamedElement> = element.parent.namedChildren(),
) {
    val duplicateNamedChildren = getDuplicatedNamedChildren(scopeNamedChildren)
    if (element.name !in duplicateNamedChildren.map { it.name }) {
        return
    }
    val identifier = element.nameIdentifier ?: element
    holder.createErrorAnnotation(identifier, "Duplicate definitions with name `${element.name}`")
}

private fun checkFunctionSignatureDuplicates(
    holder: MvAnnotationHolder,
    fn: MvFunction,
) {
    val fnSignatures =
        fn.module?.allFunctions()
            ?: fn.script?.allFunctions()
            ?: emptyList()
    val duplicateSignatures = getDuplicates(fnSignatures.asSequence())

    if (fn.name !in duplicateSignatures.map { it.name }) {
        return
    }
    val identifier = fn.nameIdentifier ?: fn
    holder.createErrorAnnotation(identifier, "Duplicate definitions with name `${fn.name}`")
}

private fun checkStructDuplicates(
    holder: MvAnnotationHolder,
    struct: MvStruct_,
) {
    val duplicateSignatures = getDuplicates(struct.module.structs().asSequence())
    if (struct.name !in duplicateSignatures.map { it.name }) {
        return
    }
    val identifier = struct.nameIdentifier ?: struct
    holder.createErrorAnnotation(identifier, "Duplicate definitions with name `${struct.name}`")
}

private fun getDuplicates(elements: Sequence<MvNamedElement>): Set<MvNamedElement> {
    return elements
        .groupBy { it.name }
        .map { it.value }
        .filter { it.size > 1 }
        .flatten()
        .toSet()
}

private fun getDuplicatedNamedChildren(namedChildren: Sequence<MvNamedElement>): Set<MvNamedElement> {
    val notNullNamedChildren = namedChildren.filter { it.name != null }
    return notNullNamedChildren
        .groupBy { it.name }
        .map { it.value }
        .filter { it.size > 1 }
        .flatten()
        .toSet()
}

private fun PsiElement.namedChildren(): Sequence<MvNamedElement> {
    return this.children.filterIsInstance<MvNamedElement>().asSequence()
}

private fun warnOnBuiltInFunctionName(holder: MvAnnotationHolder, element: MvNamedElement) {
    val nameElement = element.nameElement ?: return
    val name = element.name ?: return
    if (name in BUILTIN_FUNCTIONS) {
        holder.createErrorAnnotation(nameElement, "Invalid function name: `$name` is a built-in function")
    }
}
