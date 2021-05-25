package org.move.lang.core.types

import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.MoveStructSignature
import org.move.lang.core.psi.ext.abilities
import org.move.lang.core.psi.ext.ability
import org.move.lang.core.psi.ext.structDef

enum class Ability {
    DROP, COPY, STORE, KEY;

    fun label(): String = this.name.toLowerCase()

    fun requires(): Ability {
        return when (this) {
            DROP -> DROP
            COPY -> COPY
            KEY, STORE -> STORE
        }
    }

    companion object {
        fun all(): Set<Ability> = setOf(DROP, COPY, STORE, KEY)
    }
}

sealed class BaseType {
    abstract fun name(): String
    abstract fun abilities(): Set<Ability>
}

class PrimitiveType : BaseType() {
    override fun name(): String = TODO("Not yet implemented")
    override fun abilities(): Set<Ability> = Ability.all()
}

class RefType(val referredType: BaseType) : BaseType() {

    override fun name(): String = referredType.name()

    override fun abilities(): Set<Ability> = referredType.abilities()

    fun referredStructDef(): MoveStructDef? =
        when (referredType) {
            is StructType -> referredType.structDef()
            is RefType -> referredType.referredStructDef()
            else -> null
        }
}

class StructType(private val structSignature: MoveStructSignature) : BaseType() {

    override fun name(): String = structSignature.name ?: ""

    override fun abilities(): Set<Ability> {
        return this.structSignature.abilities.mapNotNull { it.ability }.toSet()
    }

    fun structDef(): MoveStructDef? {
        return this.structSignature.structDef
    }
}

class UnresolvedType : BaseType() {

    override fun name(): String = TODO("Not yet implemented")

    override fun abilities(): Set<Ability> = Ability.all()
}

class TypeParamType : BaseType() {

    override fun name(): String = TODO("Not yet implemented")

    override fun abilities(): Set<Ability> = Ability.all()
}
