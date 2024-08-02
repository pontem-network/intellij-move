package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.move.ide.presentation.fullname
import org.move.ide.presentation.itemDeclaredInModule
import org.move.ide.utils.functionSignature
import org.move.ide.utils.getSignature
import org.move.lang.MvElementTypes.R_PAREN
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.address
import org.move.lang.core.types.infer.descendantHasTypeError
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.TyCallable
import org.move.lang.core.types.ty.TyFunction
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

            override fun visitStructField(o: MvStructField) = checkDuplicates(moveHolder, o)

            override fun visitPath(o: MvPath) = checkMethodOrPath(o, moveHolder)
            override fun visitMethodCall(o: MvMethodCall) = checkMethodOrPath(o, moveHolder)

            override fun visitCallExpr(callExpr: MvCallExpr) {
                val msl = callExpr.path.isMslScope
                if (msl) return

                val outerFunction = callExpr.containingFunction ?: return
                if (outerFunction.isInline) return

                val path = callExpr.path
                val referenceName = path.referenceName ?: return
                val item = path.reference?.resolve() ?: return

                if (item is MvFunction && referenceName in GLOBAL_STORAGE_ACCESS_FUNCTIONS) {
                    val explicitTypeArgs = path.typeArguments
                    val currentModule = callExpr.containingModule ?: return
                    for (typeArg in explicitTypeArgs) {
                        val typeArgTy = typeArg.type.loweredType(false)
                        if (typeArgTy !is TyUnknown && !typeArgTy.itemDeclaredInModule(currentModule)) {
                            val typeName = typeArgTy.fullname()
                            Diagnostic
                                .StorageAccessIsNotAllowed(path, typeName)
                                .addToHolder(moveHolder)
                        }
                    }
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

            override fun visitValueArgumentList(arguments: MvValueArgumentList) {
                val parentCallable = arguments.parent
                val expectedCount =
                    when (parentCallable) {
                        is MvCallExpr -> {
                            val msl = parentCallable.path.isMslScope
                            val callTy =
                                parentCallable.inference(msl)?.getCallableType(parentCallable) as? TyCallable
                                    ?: return
                            callTy.paramTypes.size
                        }
                        is MvMethodCall -> {
                            val msl = parentCallable.isMslScope
                            val callTy =
                                parentCallable.inference(msl)?.getCallableType(parentCallable) as? TyCallable
                                    ?: return
                            // 1 for self
                            callTy.paramTypes.size - 1
                        }
                        is MvAssertBangExpr -> {
                            if (parentCallable.identifier.text == "assert") {
                                2
                            } else {
                                return
                            }
                        }
                        else -> return
                    }

                val valueArguments = arguments.valueArgumentList
                if (valueArguments.any { it.expr == null }) return

                val argumentExprs = valueArguments.map { it.expr!! }
                val realCount = argumentExprs.size

                when {
                    realCount < expectedCount -> {
                        val target = arguments.findFirstChildByType(R_PAREN) ?: arguments
                        Diagnostic
                            .ValueArgumentsNumberMismatch(target, expectedCount, realCount)
                            .addToHolder(moveHolder)
                        return
                    }
                    realCount > expectedCount -> {
                        argumentExprs
                            .drop(expectedCount)
                            .forEach {
                                Diagnostic
                                    .ValueArgumentsNumberMismatch(it, expectedCount, realCount)
                                    .addToHolder(moveHolder)
                            }
                        return
                    }
                }
            }

            override fun visitStructPat(o: MvStructPat) {
                val nameElement = o.path.referenceNameElement ?: return
                val refStruct = o.path.maybeStruct ?: return
                val fieldNames = o.patFields.map { it.referenceName }
                checkMissingFields(
                    moveHolder, nameElement, fieldNames.toSet(), refStruct
                )
            }

            override fun visitStructLitExpr(o: MvStructLitExpr) {
                val nameElement = o.path.referenceNameElement ?: return
                val struct = o.path.maybeStruct ?: return
                checkMissingFields(
                    moveHolder, nameElement, o.fieldNames.toSet(), struct
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
        val moveProj = mod.moveProject ?: return
        val addressIdent = mod.address(moveProj) ?: return
        val modIdent = Pair(addressIdent.text(), modName)
        val file = mod.containingMoveFile ?: return
        val duplicateIdents =
            file.modules()
                .filter { it.name != null }
                .groupBy { Pair(it.address(moveProj)?.text(), it.name) }
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
        val owner = const.parent?.parent ?: return
        val allConsts = when (owner) {
            is MvModule -> owner.consts()
            is MvScript -> owner.consts()
            else -> return
        }
        checkDuplicates(holder, const, allConsts.asSequence())
    }

    private fun checkMethodOrPath(methodOrPath: MvMethodOrPath, holder: MvAnnotationHolder) {
        val item = methodOrPath.reference?.resolveFollowingAliases()
        val msl = methodOrPath.isMslScope
        val realCount = methodOrPath.typeArguments.size

        val parent = methodOrPath.parent
        if (item == null && methodOrPath is MvPath
            && methodOrPath.qualifier == null && methodOrPath.identifierName == "vector"
        ) {
            val expectedCount = 1
            if (realCount != expectedCount) {
                Diagnostic
                    .TypeArgumentsNumberMismatch(methodOrPath, "vector", expectedCount, realCount)
                    .addToHolder(holder)
            }
            return
        }
        val qualItem = item as? MvQualNamedElement ?: return
        val qualName = qualItem.qualName ?: return
        when {
            qualItem is MvStruct && parent is MvPathType -> {
                if (parent.ancestorStrict<MvAcquiresType>() != null) return

                if (realCount != 0) {
                    val typeArgumentList =
                        methodOrPath.typeArgumentList ?: error("cannot be null if realCount != 0")
                    checkTypeArgumentList(typeArgumentList, qualItem, holder)
                } else {
                    val expectedCount = qualItem.typeParameters.size
                    if (expectedCount != 0) {
                        Diagnostic
                            .TypeArgumentsNumberMismatch(
                                methodOrPath,
                                qualName.editorText(),
                                expectedCount,
                                realCount
                            )
                            .addToHolder(holder)
                    }
                }
            }
            qualItem is MvStruct && parent is MvStructLitExpr -> {
                // if any type param is passed, inference is disabled, so check fully
                if (realCount != 0) {
                    val typeArgumentList =
                        methodOrPath.typeArgumentList ?: error("cannot be null if realCount != 0")
                    checkTypeArgumentList(typeArgumentList, qualItem, holder)
                }
            }
            qualItem is MvFunction -> {
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
                    checkTypeArgumentList(typeArgumentList, qualItem, holder)
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
            qualItem is MvSchema && parent is MvSchemaLit -> {
                val expectedCount = qualItem.typeParameters.size
                if (realCount != 0) {
                    val typeArgumentList =
                        methodOrPath.typeArgumentList ?: error("cannot be null if realCount != 0")
                    checkTypeArgumentList(typeArgumentList, qualItem, holder)
                } else {
                    // if no type args are passed, check whether all type params are inferrable
                    if (qualItem.requiredTypeParams.isNotEmpty() && expectedCount != 0) {
                        Diagnostic
                            .TypeArgumentsNumberMismatch(
                                methodOrPath,
                                qualName.editorText(),
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
        item: MvTypeParametersOwner,
        holder: MvAnnotationHolder,
    ) {
        val qualName = (item as? MvQualNamedElement)?.qualName ?: return
        val expectedCount = item.typeParameters.size

        val itemLabel = qualName.editorText()
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
}

private fun checkMissingFields(
    holder: MvAnnotationHolder,
    target: PsiElement,
    providedFieldNames: Set<String>,
    referredStruct: MvStruct,
) {
    if ((referredStruct.fieldNames.toSet() - providedFieldNames).isNotEmpty()) {
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
        fn.module?.allFunctions()
            ?: fn.script?.allFunctions()
            ?: emptyList()
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
