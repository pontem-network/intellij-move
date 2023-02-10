package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.move.ide.presentation.canBeAcquiredInModule
import org.move.ide.presentation.fullname
import org.move.lang.MvElementTypes.R_PAREN
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.infer.itemContext
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.moveProject
import org.move.lang.utils.MvDiagnostic
import org.move.lang.utils.addToHolder

class MvErrorAnnotator : MvAnnotator() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val moveHolder = MvAnnotationHolder(holder)
        val visitor = object : MvVisitor() {
            override fun visitConst(o: MvConst) = checkConstDef(moveHolder, o)

            override fun visitFunction(o: MvFunction) = checkFunction(moveHolder, o)

            override fun visitStruct(o: MvStruct) = checkStruct(moveHolder, o)

            override fun visitModule(o: MvModule) = checkModuleDef(moveHolder, o)

            override fun visitStructField(o: MvStructField) = checkDuplicates(moveHolder, o)

            override fun visitPath(path: MvPath) {
                val item = path.reference?.resolve()
                val realCount = path.typeArguments.size
                val parent = path.parent
                when {
                    item == null && path.identifierName == "vector" -> {
                        val expectedCount = 1
                        if (realCount != expectedCount) {
                            MvDiagnostic
                                .TypeArgumentsNumberMismatch(path, "vector", expectedCount, realCount)
                                .addToHolder(moveHolder)
                        }
                    }
                    item is MvStruct && parent is MvPathType -> {
                        if (parent.ancestorStrict<MvAcquiresType>() != null) return

                        val expectedCount = item.typeParameters.size
                        val label = item.fqName
                        if (expectedCount != realCount) {
                            MvDiagnostic
                                .TypeArgumentsNumberMismatch(path, label, expectedCount, realCount)
                                .addToHolder(moveHolder)
                        }
                    }
                    item is MvStruct && parent is MvStructLitExpr -> {
                        // phantom type params
                        val expectedCount = item.typeParameters.size
                        if (realCount != 0) {
                            // if any type param is passed, inference is disabled, so check fully
                            if (realCount != expectedCount) {
                                val label = item.fqName
                                MvDiagnostic
                                    .TypeArgumentsNumberMismatch(path, label, expectedCount, realCount)
                                    .addToHolder(moveHolder)
                            }
                        }
                    }
                    item is MvFunction && parent is MvCallExpr -> {
                        val expectedCount = item.typeParameters.size
                        if (realCount != 0) {
                            // if any type param is passed, inference is disabled, so check fully
                            if (realCount != expectedCount) {
                                val label = item.fqName
                                MvDiagnostic
                                    .TypeArgumentsNumberMismatch(path, label, expectedCount, realCount)
                                    .addToHolder(moveHolder)
                            }
                        } else {
                            // if no type args are passed, check whether all type params are inferrable
                            if (item.requiredTypeParams.isNotEmpty() && realCount != expectedCount) {
                                MvDiagnostic.CannotInferType(path)
                                    .addToHolder(moveHolder)
                            }
                        }
                    }
                    item is MvSchema && parent is MvSchemaLit -> {
                        val expectedCount = item.typeParameters.size
                        if (realCount != 0) {
                            // if any type param is passed, inference is disabled, so check fully
                            if (realCount != expectedCount) {
                                val label = item.fqName
                                MvDiagnostic
                                    .TypeArgumentsNumberMismatch(path, label, expectedCount, realCount)
                                    .addToHolder(moveHolder)
                            }
                        } else {
                            // if no type args are passed, check whether all type params are inferrable
                            if (item.requiredTypeParams.isNotEmpty() && realCount != expectedCount) {
                                MvDiagnostic
                                    .TypeArgumentsNumberMismatch(path, item.fqName, expectedCount, realCount)
                                    .addToHolder(moveHolder)
                            }
                        }
                    }
                }
            }

            override fun visitCallExpr(o: MvCallExpr) {
                if (o.isMsl()) return
                if (o.path.referenceName in GLOBAL_STORAGE_ACCESS_FUNCTIONS) {
                    val explicitTypeArgs = o.typeArguments
                    val currentModule = o.containingModule ?: return
                    val itemContext = currentModule.itemContext(false)
                    for (typeArg in explicitTypeArgs) {
                        val typeArgTy = itemContext.getTypeTy(typeArg.type)
                        if (typeArgTy !is TyUnknown && !typeArgTy.canBeAcquiredInModule(currentModule)) {
                            val typeName = typeArgTy.fullname()
                            holder.newAnnotation(
                                HighlightSeverity.ERROR,
                                "The type '$typeName' was not declared in the current module. " +
                                        "Global storage access is internal to the module"
                            )
                                .range(o.path)
                                .create()
                        }
                    }
                }
            }

            override fun visitValueArgumentList(arguments: MvValueArgumentList) {
                val callExpr = arguments.parent as? MvCallExpr ?: return
                val function = callExpr.path.reference?.resolve() as? MvFunction ?: return

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
                val fieldNames = o.fields.map { it.referenceName }
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

    private fun checkModuleDef(holder: MvAnnotationHolder, mod: MvModule) {
        val moveProj = mod.moveProject ?: return
        val addressIdent = mod.address()?.toAddress(moveProj) ?: return
        val modIdent = Pair(addressIdent, mod.name)
        val file = mod.containingMoveFile ?: return
        val duplicateIdents =
            file.modules()
                .filter { it.name != null }
                .groupBy { Pair(it.address()?.toAddress(), it.name) }
                .filter { it.value.size > 1 }
                .map { it.key }
                .toSet()
        if (modIdent !in duplicateIdents) return

        val identifier = mod.nameIdentifier ?: mod
        holder.createErrorAnnotation(identifier, "Duplicate definitions with name `${mod.name}`")
    }

    private fun checkConstDef(holder: MvAnnotationHolder, const: MvConst) {
        val binding = const.bindingPat ?: return
        val owner = const.parent?.parent ?: return
        val allBindings = when (owner) {
            is MvModule -> owner.constBindings()
            is MvScript -> owner.constBindings()
            else -> return
        }
        checkDuplicates(holder, binding, allBindings.asSequence())
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
