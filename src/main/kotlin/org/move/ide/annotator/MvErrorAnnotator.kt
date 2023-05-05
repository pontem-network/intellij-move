package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.move.ide.presentation.canBeAcquiredInModule
import org.move.ide.presentation.fullname
import org.move.ide.utils.functionSignature
import org.move.ide.utils.signature
import org.move.lang.MvElementTypes.R_PAREN
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.address
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.core.types.ty.hasTyInfer
import org.move.lang.moveProject
import org.move.lang.utils.MvDiagnostic
import org.move.lang.utils.addToHolder

class MvErrorAnnotator : MvAnnotatorBase() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val moveHolder = MvAnnotationHolder(holder)
        val visitor = object : MvVisitor() {
            override fun visitConst(o: MvConst) = checkConstDef(moveHolder, o)

            override fun visitFunction(o: MvFunction) = checkFunction(moveHolder, o)

            override fun visitStruct(o: MvStruct) = checkStruct(moveHolder, o)

            override fun visitModule(o: MvModule) = checkModuleDef(moveHolder, o)

            override fun visitStructField(o: MvStructField) = checkDuplicates(moveHolder, o)

            override fun visitPath(path: MvPath) {
                val item = path.reference?.resolveWithAliases()
                val msl = path.isMsl()
                val realCount = path.typeArguments.size
                val parent = path.parent
                if (item == null && path.isLocal && path.identifierName == "vector") {
                    val expectedCount = 1
                    if (realCount != expectedCount) {
                        MvDiagnostic
                            .TypeArgumentsNumberMismatch(path, "vector", expectedCount, realCount)
                            .addToHolder(moveHolder)
                    }
                } else {
                    val qualItem = item as? MvQualNamedElement ?: return
                    val qualName = qualItem.qualName ?: return
                    when {
                        qualItem is MvStruct && parent is MvPathType -> {
                            if (parent.ancestorStrict<MvAcquiresType>() != null) return

                            val expectedCount = qualItem.typeParameters.size
                            if (expectedCount != realCount) {
                                MvDiagnostic
                                    .TypeArgumentsNumberMismatch(
                                        path,
                                        qualName.editorText(),
                                        expectedCount,
                                        realCount
                                    )
                                    .addToHolder(moveHolder)
                            }
                        }
                        qualItem is MvStruct && parent is MvStructLitExpr -> {
                            // phantom type params
                            val expectedCount = qualItem.typeParameters.size
                            if (realCount != 0) {
                                // if any type param is passed, inference is disabled, so check fully
                                if (realCount != expectedCount) {
                                    MvDiagnostic
                                        .TypeArgumentsNumberMismatch(
                                            path,
                                            qualName.editorText(),
                                            expectedCount,
                                            realCount
                                        )
                                        .addToHolder(moveHolder)
                                }
                            }
                        }
                        qualItem is MvFunction && parent is MvCallExpr -> {
                            val expectedCount = qualItem.typeParameters.size
                            if (realCount != 0) {
                                // if any type param is passed, inference is disabled, so check fully
                                if (realCount != expectedCount) {
                                    MvDiagnostic
                                        .TypeArgumentsNumberMismatch(
                                            path,
                                            qualName.editorText(),
                                            expectedCount,
                                            realCount
                                        )
                                        .addToHolder(moveHolder)
                                }
                            } else {
                                val callTy = parent.inference(msl)?.getCallExprType(parent)
                                        as? TyFunction ?: return
                                // if no type args are passed, check whether all type params are inferrable
                                if (callTy.needsTypeAnnotation()) {
                                    MvDiagnostic
                                        .NeedsTypeAnnotation(path)
                                        .addToHolder(moveHolder)
                                }
                            }
                        }
                        qualItem is MvSchema && parent is MvSchemaLit -> {
//                        qualItem is MvSchema
//                                && (parent is MvSchemaLit || parent is MvRefExpr) -> {
                            val expectedCount = qualItem.typeParameters.size
                            if (realCount != 0) {
                                // if any type param is passed, inference is disabled, so check fully
                                if (realCount != expectedCount) {
                                    MvDiagnostic
                                        .TypeArgumentsNumberMismatch(
                                            path,
                                            qualName.editorText(),
                                            expectedCount,
                                            realCount
                                        )
                                        .addToHolder(moveHolder)
                                }
                            } else {
                                // if no type args are passed, check whether all type params are inferrable
                                if (qualItem.requiredTypeParams.isNotEmpty() && realCount != expectedCount) {
                                    MvDiagnostic
                                        .TypeArgumentsNumberMismatch(
                                            path,
                                            qualName.editorText(),
                                            expectedCount,
                                            realCount
                                        )
                                        .addToHolder(moveHolder)
                                }
                            }
                        }
                    }
                }
            }

            override fun visitCallExpr(callExpr: MvCallExpr) {
                val msl = callExpr.isMsl()
                if (msl) return

                val outerFunction = callExpr.containingFunction ?: return
                if (outerFunction.isInline) return

                val path = callExpr.path
                val referenceName = path.referenceName ?: return
                val item = path.reference?.resolve() ?: return

//                val itemContext = outerFunction.outerItemContext(msl)

                if (item is MvFunction && referenceName in GLOBAL_STORAGE_ACCESS_FUNCTIONS) {
                    val explicitTypeArgs = path.typeArguments
                    val currentModule = callExpr.containingModule ?: return
//                    val inferenceCtx = callExpr.maybeInferenceContext(false) ?: return
                    for (typeArg in explicitTypeArgs) {
//                        val typeArgTy = inferenceCtx.getTypeTy(typeArg.type)
                        val typeArgTy = typeArg.type.loweredType(msl)
//                        val typeArgTy = itemContext.rawType(typeArg.type)
                        if (typeArgTy !is TyUnknown && !typeArgTy.canBeAcquiredInModule(currentModule)) {
                            val typeName = typeArgTy.fullname()
                            MvDiagnostic
                                .StorageAccessIsNotAllowed(path, typeName)
                                .addToHolder(moveHolder)
                        }
                    }
                }
            }

            override fun visitItemSpec(itemSpec: MvItemSpec) {
                val funcItem = itemSpec.funcItem ?: return
                val funcSignature = funcItem.signature ?: return
                val itemSpecSignature = itemSpec.itemSpecSignature ?: return

                val specSignature = itemSpecSignature.functionSignature
                if (funcSignature != specSignature) {
                    MvDiagnostic
                        .FunctionSignatureMismatch(itemSpec)
                        .addToHolder(moveHolder)
                }
            }

            override fun visitValueArgumentList(arguments: MvValueArgumentList) {
                val callExpr = arguments.parent as? MvCallExpr ?: return
                val function = callExpr.path.reference?.resolveWithAliases() as? MvFunctionLike ?: return

                val expectedCount = function.parameters.size
                val realCount = arguments.valueArgumentList.size
                val errorMessage =
                    "This function takes $expectedCount ${
                        pluralise(
                            expectedCount,
                            "parameter",
                            "parameters"
                        )
                    } " +
                            "but $realCount ${pluralise(realCount, "parameter", "parameters")} " +
                            "${pluralise(realCount, "was", "were")} supplied"
                when {
                    realCount < expectedCount -> {
                        val target = arguments.findFirstChildByType(R_PAREN) ?: arguments
                        holder.newAnnotation(HighlightSeverity.ERROR, errorMessage)
                            .range(target)
                            .create()
                        return
                    }
                    realCount > expectedCount -> {
                        arguments.valueArgumentList.drop(expectedCount)
                            .forEach {
                                holder.newAnnotation(HighlightSeverity.ERROR, errorMessage)
                                    .range(it)
                                    .create()
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

            override fun visitCastExpr(castExpr: MvCastExpr) {
                val parent = castExpr.parent
                if (parent !is MvParensExpr) {
                    MvDiagnostic
                        .ParensAreRequiredForCastExpr(castExpr)
                        .addToHolder(moveHolder)
                }
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

    private fun checkModuleDef(holder: MvAnnotationHolder, mod: MvModule) {
        val moveProj = mod.moveProject ?: return
        val addressIdent = mod.address(moveProj) ?: return
        val modIdent = Pair(addressIdent.text(), mod.name)
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
        holder.createErrorAnnotation(identifier, "Duplicate definitions with name `${mod.name}`")
    }

    private fun checkConstDef(holder: MvAnnotationHolder, const: MvConst) {
//        val binding = const.bindingPat ?: return
        val owner = const.parent?.parent ?: return
        val allConsts = when (owner) {
            is MvModule -> owner.consts()
            is MvScript -> owner.consts()
            else -> return
        }
        checkDuplicates(holder, const, allConsts.asSequence())
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
    val duplicateNamedChildren = getDuplicatedNamedChildren(scopeNamedChildren)
    if (element.name !in duplicateNamedChildren.map { it.name }) {
        return
    }
    val identifier = element.nameElement ?: element
    holder.createErrorAnnotation(identifier, "Duplicate definitions with name `${element.name}`")
}

private fun checkFunctionDuplicates(
    holder: MvAnnotationHolder,
    fn: MvFunction,
) {
    val functions =
        fn.module?.allFunctions()
            ?: fn.script?.allFunctions()
            ?: emptyList()
    val duplicateFunctions = getDuplicates(functions.asSequence())

    if (fn.name !in duplicateFunctions.map { it.name }) {
        return
    }
    val identifier = fn.nameIdentifier ?: fn
    holder.createErrorAnnotation(identifier, "Duplicate definitions with name `${fn.name}`")
}

private fun checkStructDuplicates(
    holder: MvAnnotationHolder,
    struct: MvStruct,
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
