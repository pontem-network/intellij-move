package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.lang.core.psi.mixins.declaredTy
import org.move.lang.core.types.infer.foldTyTypeParameterWith
import org.move.stdext.chain

val MvSchema.specBlock: MvSpecBlock? get() = this.childOfType()

val MvSchema.module: MvModuleDef
    get() {
        val moduleBlock = this.parent
        return moduleBlock.parent as MvModuleDef
    }

val MvSchema.fqName: String
    get() {
        val moduleFqName = this.module.fqName?.let { "$it::" }
        val name = this.name ?: "<unknown>"
        return moduleFqName + name
    }

val MvSchema.typeParams get() = typeParameterList?.typeParameterList.orEmpty()

val MvSchema.requiredTypeParams: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        this.fieldStmts
            .map { it.declaredTy(true) }
            .forEach {
                it.foldTyTypeParameterWith { paramTy -> usedTypeParams.add(paramTy.parameter); paramTy }
            }
        return this.typeParameters.filter { it !in usedTypeParams }
    }

val MvSchema.fieldStmts get() = this.specBlock?.schemaFields().orEmpty()

val MvSchema.fieldBindings get() = this.fieldStmts.map { it.bindingPat }
