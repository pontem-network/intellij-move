package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.lang.core.types.infer.foldTyTypeParameterWith

val MvSchema.specBlock: MvItemSpecBlock? get() = this.childOfType()

val MvSchema.module: MvModule
    get() {
        val moduleBlock = this.parent
        return moduleBlock.parent as MvModule
    }

val MvSchema.typeParams get() = typeParameterList?.typeParameterList.orEmpty()

val MvSchema.requiredTypeParams: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        this.fieldStmts
            .map { it.declaredTy(true) }
            .forEach {
                it.foldTyTypeParameterWith { paramTy -> usedTypeParams.add(paramTy.origin); paramTy }
            }
        return this.typeParameters.filter { it !in usedTypeParams }
    }

val MvSchema.fieldStmts get() = this.specBlock?.schemaFields().orEmpty()

val MvSchema.fieldBindings get() = this.fieldStmts.map { it.bindingPat }
