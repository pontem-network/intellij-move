package org.move.lang.core.types

import org.move.lang.core.psi.*
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
    abstract fun fullname(): String
    abstract fun abilities(): Set<Ability>
    abstract fun definingModule(): MoveModuleDef?

    fun typeLabel(relativeTo: MoveElement): String {
        val exprTypeModule = this.definingModule()
        return if (exprTypeModule != null
            && exprTypeModule != relativeTo.containingModule
        ) {
            this.fullname()
        } else {
            this.name()
        }
    }
}

class PrimitiveType(private val name: String) : BaseType() {
    override fun name(): String = name
    override fun fullname(): String = name()
    override fun abilities(): Set<Ability> = Ability.all()
    override fun definingModule(): MoveModuleDef? = null
}

class VoidType : BaseType() {
    override fun name(): String = "void"
    override fun fullname(): String = "void"
    override fun abilities(): Set<Ability> = emptySet()
    override fun definingModule(): MoveModuleDef? = null
}

class RefType(
    val referredType: BaseType,
    val mutable: Boolean
) : BaseType() {

    private fun prefix(): String {
        return if (mutable) "&mut " else "&"
    }

    override fun name(): String = prefix() + referredType.name()
    override fun fullname(): String = prefix() + referredType.fullname()
    override fun abilities(): Set<Ability> = referredType.abilities()
    override fun definingModule(): MoveModuleDef? = referredType.definingModule()

    fun referredTypeName(): String = referredType.name()
    fun referredTypeFullName(): String = referredType.fullname()

    fun referredStructDef(): MoveStructDef? =
        when (referredType) {
            is StructType -> referredType.structDef()
            is RefType -> referredType.referredStructDef()
            else -> null
        }
}

class StructType(private val structSignature: MoveStructSignature) : BaseType() {

    override fun name(): String = structSignature.name ?: ""

    override fun fullname(): String {
        val moduleName = structSignature.containingModule?.name ?: return this.name()
        val address = structSignature.containingAddress.text
        return "$address::$moduleName::${this.name()}"
    }

    override fun abilities(): Set<Ability> {
        return this.structSignature.abilities.mapNotNull { it.ability }.toSet()
    }

    override fun definingModule(): MoveModuleDef? {
        return this.structSignature.containingModule
    }

    fun structDef(): MoveStructDef? {
        return this.structSignature.structDef
    }
}

class TypeParamType(private val typeParam: MoveTypeParameter) : BaseType() {

    override fun name(): String = typeParam.name ?: ""
    override fun fullname(): String = this.name()
    override fun definingModule(): MoveModuleDef? = null

    override fun abilities(): Set<Ability> {
        return typeParam.abilities.mapNotNull { it.ability }.toSet()
    }
}

fun isCompatibleTypes(expectedType: BaseType, actualType: BaseType): Boolean {
    if (expectedType::class != actualType::class) return false
    when {
        expectedType is RefType && actualType is RefType -> {
            if (expectedType.fullname() == actualType.fullname()) return true

            val refsCompatible =
                isCompatibleTypes(
                    expectedType.referredType,
                    actualType.referredType
                )
            return refsCompatible && (!expectedType.mutable && actualType.mutable)
        }
    }
    return expectedType::class == actualType::class
            && expectedType.fullname() == actualType.fullname()
}
