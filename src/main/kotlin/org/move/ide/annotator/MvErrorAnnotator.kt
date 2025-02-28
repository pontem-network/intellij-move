package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.move.ide.presentation.declaringModule
import org.move.ide.presentation.fullname
import org.move.ide.utils.functionSignature
import org.move.ide.utils.getSignature
import org.move.lang.MvElementTypes.R_PAREN
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.PathKind
import org.move.lang.core.resolve.pathKind
import org.move.lang.core.types.address
import org.move.lang.core.types.fullname
import org.move.lang.core.types.infer.descendantHasTypeError
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.fqName
import org.move.lang.core.types.ty.TyCallable
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyTypeParameter
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.moveProject
import org.move.lang.utils.Diagnostic
import org.move.lang.utils.addToHolder

class MvErrorAnnotator: MvAnnotatorBase() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val moveHolder = MvAnnotationHolder(holder)
        val visitor = object: MvVisitor() {
            override fun visitConst(o: MvConst) = checkConstDef(moveHolder, o)

            override fun visitFunction(o: MvFunction) = checkFunction(moveHolder, o)

            override fun visitStruct(o: MvStruct) = checkStruct(moveHolder, o)

            override fun visitModule(o: MvModule) = checkModuleDef(moveHolder, o)

            override fun visitNamedFieldDecl(o: MvNamedFieldDecl) = checkDuplicates(moveHolder, o)

            override fun visitPath(o: MvPath) = checkMethodOrPath(o, moveHolder)
            override fun visitMethodCall(o: MvMethodCall) = checkMethodOrPath(o, moveHolder)

            override fun visitCallExpr(callExpr: MvCallExpr) {
                val msl = callExpr.path.isMslScope
                if (msl) return

                val outerFunction = callExpr.containingFunction ?: return
                if (outerFunction.isInline) return

                val path = callExpr.path
                val item = path.reference?.resolve() ?: return
                if (item !is MvFunction) return

                val referenceName = path.referenceName ?: return
                if (referenceName !in GLOBAL_STORAGE_ACCESS_FUNCTIONS) return

                val currentModule = callExpr.containingModule ?: return
                val typeArg = path.typeArguments.singleOrNull() ?: return

                val typeArgTy = typeArg.type.loweredType(false)
                if (typeArgTy is TyUnknown) return

                when {
                    typeArgTy is TyTypeParameter -> {
                        val itemName = typeArgTy.origin.name ?: return
                        Diagnostic.StorageAccessError.WrongItem(path, itemName)
                            .addToHolder(moveHolder)
                        return
                    }
                    // todo: else
                }

                val itemModule = typeArgTy.declaringModule() ?: return
                if (currentModule != itemModule) {
                    val itemModuleName = itemModule.fullname() ?: return
                    var moduleQualTypeName = typeArgTy.fullname()
                    if (moduleQualTypeName.split("::").size == 3) {
                        // fq name
                        moduleQualTypeName =
                            moduleQualTypeName.split("::").drop(1).joinToString("::")
                    }
                    Diagnostic.StorageAccessError.WrongModule(path, itemModuleName, moduleQualTypeName)
                        .addToHolder(moveHolder)
                }
            }

            override fun visitItemSpec(itemSpec: MvItemSpec) {
                val funcItem = itemSpec.funcItem ?: return
                val funcSignature = funcItem.getSignature() ?: return
                val itemSpecSignature = itemSpec.itemSpecSignature ?: return

                val specSignature = itemSpecSignature.functionSignature
                if (funcSignature != specSignature) {
                    Diagnostic
                        .FunctionSignatureMismatch(itemSpec)
                        .addToHolder(moveHolder)
                }
            }

            override fun visitValueArgumentList(o: MvValueArgumentList) = checkValueArgumentList(moveHolder, o)

            override fun visitPatStruct(patStruct: MvPatStruct) {
                val declaration =
                    patStruct.path.reference?.resolveFollowingAliases() as? MvFieldsOwner ?: return
                val bodyFieldNames = patStruct.fieldNames
                val missingFields = declaration.namedFields.filter { it.name !in bodyFieldNames }
                if (missingFields.isNotEmpty() && patStruct.patRest == null) {
                    Diagnostic.MissingFieldsInStructPattern(patStruct, declaration, missingFields)
                        .addToHolder(moveHolder)
                }
            }

            override fun visitPatTupleStruct(o: MvPatTupleStruct) = checkPatTupleStruct(moveHolder, o)

            override fun visitStructLitExpr(o: MvStructLitExpr) {
                val nameElement = o.path.referenceNameElement ?: return
                val struct = o.path.maybeStruct ?: return
                checkMissingFields(
                    moveHolder, nameElement, o.providedFieldNames.toSet(), struct
                )
            }
        }
        element.accept(visitor)
    }

    private fun checkStruct(holder: MvAnnotationHolder, struct: MvStruct) {
        checkStructDuplicates(holder, struct)
    }

    private fun checkFunction(holder: MvAnnotationHolder, function: MvFunction) {
        checkFunctionDuplicates(holder, function)
        warnOnBuiltInFunctionName(holder, function)
    }

    private fun checkModuleDef(moveHolder: MvAnnotationHolder, mod: MvModule) {
        val modName = mod.name ?: return
        val moveProject = mod.moveProject ?: return
        val addressIdent = mod.address(moveProject) ?: return
        val modIdent = Pair(addressIdent.text(), modName)
        val file = mod.containingMoveFile ?: return
        val duplicateIdents =
            file.modules()
                .filter { it.name != null }
                .groupBy { Pair(it.address(moveProject)?.text(), it.name) }
                .filter { it.value.size > 1 }
                .map { it.key }
                .toSet()
        if (modIdent !in duplicateIdents) return

        val identifier = mod.nameIdentifier ?: mod
        Diagnostic
            .DuplicateDefinitions(identifier, modName)
            .addToHolder(moveHolder)
    }

    private fun checkConstDef(holder: MvAnnotationHolder, const: MvConst) {
        val owner = const.parent ?: return
        val allConsts = when (owner) {
            is MvModule -> owner.constList
            is MvScript -> owner.constList
            else -> return
        }
        checkDuplicates(holder, const, allConsts.asSequence())
    }

    private fun checkMethodOrPath(methodOrPath: MvMethodOrPath, holder: MvAnnotationHolder) {
        val realCount = methodOrPath.typeArguments.size
        if (methodOrPath is MvPath && methodOrPath.identifierName == "vector") {
            // try to check whether it's a `vector<>` type instantiation
            // and it has a single type argument for the element type
            run {
                val rootPath = methodOrPath.rootPath()
                // only if `vector` is 1-element path
                if (rootPath.pathKind() !is PathKind.UnqualifiedPath) return@run
                // relevant only in type position
                if (rootPath.parent !is MvPathType) return@run
                if (realCount != 1) {
                    Diagnostic
                        .TypeArgumentsNumberMismatch(methodOrPath, "vector", 1, realCount)
                        .addToHolder(holder)
                }
                return
            }
        }

        val msl = methodOrPath.isMslScope
        val parent = methodOrPath.parent
        val item = methodOrPath.reference?.resolveFollowingAliases() ?: return

        val fqName = item.fqName() ?: return
        when {
            item is MvStruct && parent is MvPathType -> {
                if (parent.ancestorStrict<MvAcquiresType>() != null) return

                if (realCount != 0) {
                    val typeArgumentList =
                        methodOrPath.typeArgumentList ?: error("cannot be null if realCount != 0")
                    checkTypeArgumentList(typeArgumentList, item, holder)
                } else {
                    val expectedCount = item.typeParameters.size
                    if (expectedCount != 0) {
                        Diagnostic
                            .TypeArgumentsNumberMismatch(
                                methodOrPath,
                                fqName.declarationText(),
                                expectedCount,
                                realCount
                            )
                            .addToHolder(holder)
                    }
                }
            }
            item is MvStruct && parent is MvStructLitExpr -> {
                // if any type param is passed, inference is disabled, so check fully
                if (realCount != 0) {
                    val typeArgumentList =
                        methodOrPath.typeArgumentList ?: error("cannot be null if realCount != 0")
                    checkTypeArgumentList(typeArgumentList, item, holder)
                }
            }
            item is MvFunction -> {
                val callable =
                    when (parent) {
                        is MvCallExpr -> parent
                        is MvDotExpr -> parent.methodCall
                        else -> null
                    } ?: return
                if (realCount != 0) {
                    // if any type param is passed, inference is disabled, so check fully
                    val typeArgumentList =
                        methodOrPath.typeArgumentList ?: error("cannot be null if realCount != 0")
                    checkTypeArgumentList(typeArgumentList, item, holder)
                } else {
                    val inference = callable.inference(msl) ?: return
                    if (callable.descendantHasTypeError(inference.typeErrors)) {
                        return
                    }
                    val callTy = inference.getCallableType(callable) as? TyFunction ?: return
                    // if no type args are passed, check whether all type params are inferrable
                    if (callTy.needsTypeAnnotation()) {
                        val annotatedItem =
                            if (methodOrPath is MvMethodCall) methodOrPath.identifier else methodOrPath
                        Diagnostic
                            .NeedsTypeAnnotation(annotatedItem)
                            .addToHolder(holder)
                    }
                }
            }
            item is MvSchema && parent is MvSchemaLit -> {
                val expectedCount = item.typeParameters.size
                if (realCount != 0) {
                    val typeArgumentList =
                        methodOrPath.typeArgumentList ?: error("cannot be null if realCount != 0")
                    checkTypeArgumentList(typeArgumentList, item, holder)
                } else {
                    // if no type args are passed, check whether all type params are inferrable
                    if (item.requiredTypeParams.isNotEmpty() && expectedCount != 0) {
                        Diagnostic
                            .TypeArgumentsNumberMismatch(
                                methodOrPath,
                                fqName.declarationText(),
                                expectedCount,
                                realCount
                            )
                            .addToHolder(holder)
                    }
                }
            }
        }
    }

    private fun checkTypeArgumentList(
        typeArgumentList: MvTypeArgumentList,
        item: MvGenericDeclaration,
        holder: MvAnnotationHolder,
    ) {
        val qualName = (item as? MvNamedElement)?.fqName() ?: return
        val expectedCount = item.typeParameters.size

        val itemLabel = qualName.declarationText()
        val realCount = typeArgumentList.typeArgumentList.size
        check(realCount != 0) { "Should be non-zero if typeArgumentList exists" }

        // if any type param is passed, inference is disabled, so check fully
        when {
            expectedCount == 0 -> {
                Diagnostic
                    .NoTypeArgumentsExpected(typeArgumentList, itemLabel)
                    .addToHolder(holder)
            }
            realCount < expectedCount -> {
                Diagnostic
                    .TypeArgumentsNumberMismatch(
                        typeArgumentList,
                        itemLabel,
                        expectedCount,
                        realCount
                    )
                    .addToHolder(holder)
            }
            realCount > expectedCount -> {
                typeArgumentList.typeArgumentList
                    .drop(expectedCount)
                    .forEach {
                        Diagnostic
                            .TypeArgumentsNumberMismatch(
                                it,
                                itemLabel,
                                expectedCount,
                                realCount
                            )
                            .addToHolder(holder)
                    }
            }
        }
    }

    @Suppress("ReplaceRangeStartEndInclusiveWithFirstLast")
    private fun checkValueArgumentList(holder: MvAnnotationHolder, argumentList: MvValueArgumentList) {
        val callable = argumentList.parent

        val valueArguments = argumentList.valueArgumentList
        if (valueArguments.any { it.expr == null }) return

        val argumentExprs = valueArguments.map { it.expr!! }
        val realCount = argumentExprs.size

        // use range, because assert! can have either 1 or 2 arguments
        val expectedRange =
            when (callable) {
                is MvCallExpr -> {
                    val msl = callable.path.isMslScope
                    val callTy =
                        callable.inference(msl)?.getCallableType(callable) as? TyCallable
                            ?: return
                    val count = callTy.paramTypes.size
                    IntRange(count, count)
                }
                is MvMethodCall -> {
                    val msl = callable.isMslScope
                    val callTy =
                        callable.inference(msl)?.getCallableType(callable) as? TyCallable
                            ?: return
                    // 1 for self
                    val count = callTy.paramTypes.size - 1
                    IntRange(count, count)
                }
                is MvAssertMacroExpr -> IntRange(1, 2)
                else -> return
            }

        when {
            realCount < expectedRange.start -> {
                val target = argumentList.findFirstChildByType(R_PAREN) ?: argumentList
                Diagnostic.ValueArgumentsNumberMismatch(target, expectedRange, realCount)
                    .addToHolder(holder)
                return
            }
            realCount > expectedRange.endInclusive -> {
                argumentExprs
                    .drop(expectedRange.endInclusive)
                    .forEach {
                        Diagnostic.ValueArgumentsNumberMismatch(it, expectedRange, realCount)
                            .addToHolder(holder)
                    }
                return
            }
        }
    }

    private fun checkPatTupleStruct(holder: MvAnnotationHolder, patTupleStruct: MvPatTupleStruct) {
        val declaration = patTupleStruct.path.reference?.resolveFollowingAliases() as? MvFieldsOwner ?: return

        val declarationFieldsAmount = declaration.fields.size
        // Rest is non-binding, meaning it is accepted even if all fields are already bound
        val bodyFieldsAmount = patTupleStruct.patList.filterNot { it is MvPatRest }.size

        if (bodyFieldsAmount < declarationFieldsAmount && patTupleStruct.patRest == null) {
            Diagnostic.MissingFieldsInTuplePattern(
                patTupleStruct,
                declaration,
                declarationFieldsAmount,
                bodyFieldsAmount
            ).addToHolder(holder)
        }
    }
}

private fun checkMissingFields(
    holder: MvAnnotationHolder,
    target: PsiElement,
    bodyFieldNames: Set<String>,
    declaration: MvFieldsOwner,
) {
    if ((declaration.fieldNames.toSet() - bodyFieldNames).isNotEmpty()) {
        holder.createErrorAnnotation(target, "Some fields are missing")
    }
}

private fun checkDuplicates(
    holder: MvAnnotationHolder,
    element: MvNamedElement,
    scopeNamedChildren: Sequence<MvNamedElement> = element.parent.namedChildren(),
) {
    val elementName = element.name ?: return
    val duplicateNamedChildren = getDuplicatedNamedChildren(scopeNamedChildren)
    if (elementName !in duplicateNamedChildren.map { it.name }) {
        return
    }
    val identifier = element.nameElement ?: element
    Diagnostic
        .DuplicateDefinitions(identifier, elementName)
        .addToHolder(holder)
}

private fun checkFunctionDuplicates(
    holder: MvAnnotationHolder,
    fn: MvFunction,
) {
    val fnName = fn.name ?: return
    val functions =
        fn.module?.allFunctions() ?: fn.script?.functionList ?: emptyList()
    val duplicateFunctions = getDuplicates(functions.asSequence())

    if (fnName !in duplicateFunctions.map { it.name }) {
        return
    }
    val identifier = fn.nameIdentifier ?: fn
    Diagnostic
        .DuplicateDefinitions(identifier, fnName)
        .addToHolder(holder)
}

private fun checkStructDuplicates(
    holder: MvAnnotationHolder,
    struct: MvStruct,
) {
    val structName = struct.name ?: return
    val duplicateSignatures = getDuplicates(struct.module.structs().asSequence())
    if (structName !in duplicateSignatures.map { it.name }) {
        return
    }
    val identifier = struct.nameIdentifier ?: struct
    Diagnostic
        .DuplicateDefinitions(identifier, structName)
        .addToHolder(holder)
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
