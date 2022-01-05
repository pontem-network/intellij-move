package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import org.move.lang.MvElementTypes.R_PAREN
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*

class ErrorAnnotator : MvAnnotator() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val moveHolder = MvAnnotationHolder(holder)
        val visitor = object : MvVisitor() {
            override fun visitConstDef(o: MvConstDef) = checkConstDef(moveHolder, o)

            override fun visitFunction(o: MvFunction) = checkFunction(moveHolder, o)

            override fun visitStruct_(o: MvStruct_) = checkStruct(moveHolder, o)

            override fun visitModuleDef(o: MvModuleDef) = checkModuleDef(moveHolder, o)

            override fun visitStructFieldDef(o: MvStructFieldDef) = checkDuplicates(moveHolder, o)

            override fun visitPath(path: MvPath) {
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

                val typeParamsOwner = referred as? MvTypeParametersOwner ?: return
                val expectedCount = typeParamsOwner.typeParameters.size
                val realCount = typeArguments.size

                if (expectedCount == 0 && realCount != 0) {
                    holder.createErrorAnnotation(
                        path.typeArgumentList!!,
                        "No type arguments expected",
                    )
                    return
                }
                if (realCount > expectedCount) {
                    typeArguments.drop(expectedCount).forEach {
                        holder.createErrorAnnotation(
                            it,
                            "Wrong number of type arguments: expected $expectedCount, found $realCount",
                        )
                    }
                    return
                }
            }

            override fun visitCallArgumentList(arguments: MvCallArgumentList) {
                val callExpr = arguments.parent as? MvCallExpr ?: return
                val function = callExpr.path.reference?.resolve() as? MvFunction ?: return

                val expectedCount = function.parameters.size
                val realCount = arguments.exprList.size
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
