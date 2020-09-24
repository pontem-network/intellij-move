package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.R_PAREN
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.expectedParamsCount
import org.move.lang.core.psi.ext.findFirstChildByType
import org.move.lang.core.psi.ext.identifierName
import org.move.lang.core.psi.ext.typeArguments

class ErrorAnnotator : MoveAnnotator() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val moveHolder = MoveAnnotationHolder(holder)
        val visitor = object : MoveVisitor() {
            override fun visitConstDef(o: MoveConstDef) = checkConstDef(moveHolder, o)

            override fun visitFunctionDef(o: MoveFunctionDef) = checkFunctionDef(moveHolder, o)
            override fun visitNativeFunctionDef(o: MoveNativeFunctionDef) =
                checkNativeFunctionDef(moveHolder, o)

            override fun visitModuleDef(o: MoveModuleDef) = checkModuleDef(moveHolder, o)

            override fun visitStructDef(o: MoveStructDef) = checkStructDef(moveHolder, o)
            override fun visitNativeStructDef(o: MoveNativeStructDef) = checkNativeStructDef(moveHolder, o)
            override fun visitStructFieldDef(o: MoveStructFieldDef) = checkStructFieldDef(moveHolder, o)

            override fun visitQualPath(o: MoveQualPath) = checkQualifiedPath(moveHolder, o)

            override fun visitCallArguments(o: MoveCallArguments) = checkCallArguments(moveHolder, o)
        }
        element.accept(visitor)
    }

    private fun checkFunctionDef(holder: MoveAnnotationHolder, fn: MoveFunctionDef) {
        checkDuplicates(holder, fn)
        warnOnBuiltInFunctionName(holder, fn)
    }

    private fun checkNativeFunctionDef(holder: MoveAnnotationHolder, nativeFn: MoveNativeFunctionDef) {
        checkDuplicates(holder, nativeFn)
        warnOnBuiltInFunctionName(holder, nativeFn)
    }

    private fun checkModuleDef(holder: MoveAnnotationHolder, mod: MoveModuleDef) {
        checkDuplicates(holder, mod)
    }

    private fun checkStructDef(holder: MoveAnnotationHolder, struct: MoveStructDef) {
        checkDuplicates(holder, struct)
    }

    private fun checkNativeStructDef(holder: MoveAnnotationHolder, nativeStruct: MoveNativeStructDef) {
        checkDuplicates(holder, nativeStruct)
    }

    private fun checkStructFieldDef(holder: MoveAnnotationHolder, structField: MoveStructFieldDef) {
        checkDuplicates(holder, structField)
    }

    private fun checkConstDef(holder: MoveAnnotationHolder, const: MoveConstDef) {
        checkDuplicates(holder, const)
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

private fun checkQualifiedPath(holder: MoveAnnotationHolder, qualPath: MoveQualPath) {
    val referred = (qualPath.parent as MoveQualPathReferenceElement).reference.resolve()
    if (referred == null && qualPath.identifierName == "vector") {
        if (qualPath.typeArguments.isEmpty()) {
            holder.createErrorAnnotation(qualPath.identifier, "Missing item type argument")
            return
        }
        val realCount = qualPath.typeArguments.size
        if (realCount > 1) {
            qualPath.typeArguments.drop(1).forEach {
                holder.createErrorAnnotation(it,
                    "Wrong number of type arguments: expected 1, found $realCount")
            }
            return
        }
    }
    val name = referred?.name ?: return

    when {
        referred is MoveNativeFunctionDef
                && name in BUILTIN_FUNCTIONS_WITH_REQUIRED_RESOURCE_TYPE
                && qualPath.typeArguments.isEmpty() -> {
            holder.createErrorAnnotation(qualPath, "Missing resource type argument")
            return
        }
        referred is MoveTypeParametersOwner -> {
            val expectedCount = referred.typeParameters.size
            val realCount = qualPath.typeArguments.size

            if (expectedCount == 0 && realCount != 0) {
                holder.createErrorAnnotation(qualPath.typeArgumentList!!, "No type arguments expected")
                return
            }

            if (realCount > expectedCount) {
                qualPath.typeArguments.drop(expectedCount).forEach {
                    holder.createErrorAnnotation(it,
                        "Wrong number of type arguments: expected $expectedCount, found $realCount")
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
