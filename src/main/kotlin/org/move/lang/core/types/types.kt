package org.move.lang.core.types

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.abilities
import org.move.lang.core.psi.ext.ability
import org.move.lang.core.psi.ext.structDef

typealias TypeVarsMap = Map<String, BaseType?>

interface HasType : MoveElement {
    fun resolvedType(typeVars: TypeVarsMap): BaseType?
}

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
    abstract fun compatibleWith(other: BaseType): Boolean

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

    override fun compatibleWith(other: BaseType): Boolean {
        return other is PrimitiveType && this.name == other.name()
    }
}

class IntegerType(private val precision: String? = null) : BaseType() {
    override fun name(): String = precision ?: "integer"
    override fun fullname(): String = name()
    override fun abilities(): Set<Ability> = Ability.all()
    override fun definingModule(): MoveModuleDef? = null

    override fun compatibleWith(other: BaseType): Boolean {
        if (other !is IntegerType) return false
        return this.precision == null
                || other.precision == null
                || this.precision == other.precision
    }
}

class VectorType(private val itemType: BaseType) : BaseType() {
    override fun name(): String = "vector<${itemType.fullname()}>"
    override fun fullname(): String = name()
    override fun abilities(): Set<Ability> = Ability.all()
    override fun definingModule(): MoveModuleDef? = null

    override fun compatibleWith(other: BaseType): Boolean {
        return other is VectorType && this.itemType.compatibleWith(other.itemType)
    }
}

class VoidType : BaseType() {
    override fun name(): String = "void"
    override fun fullname(): String = "void"
    override fun abilities(): Set<Ability> = emptySet()
    override fun definingModule(): MoveModuleDef? = null

    override fun compatibleWith(other: BaseType): Boolean {
        return other is VoidType
    }
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

    override fun compatibleWith(other: BaseType): Boolean {
        if (other !is RefType) return false
        if (this.fullname() == other.fullname()) return true

        return this.referredType.compatibleWith(other.referredType)
                && (!this.mutable && other.mutable)
    }

//    fun referredTypeName(): String = referredType.name()
//    fun referredTypeFullName(): String = referredType.fullname()

    fun referredStructDef(): MoveStructDef? =
        when (referredType) {
            is StructType -> referredType.structDef()
            is RefType -> referredType.referredStructDef()
            else -> null
        }
}

class StructType(
    private val structSignature: MoveStructSignature,
    val typeArgumentTypes: List<BaseType?> = emptyList()
) : BaseType() {

    override fun name(): String = structSignature.name ?: ""

    override fun fullname(): String {
//        val moduleName = structSignature.containingModule?.name ?: return this.name()
//        val address = structSignature.containingAddress.text
        var structName = structFullname()
        if (typeArgumentTypes.isNotEmpty()) {
            structName += "<"
            for ((i, typeArgumentType) in typeArgumentTypes.withIndex()) {
                structName += typeArgumentType?.fullname() ?: "unknown_type"
                if (i < typeArgumentTypes.size - 1) {
                    structName += ", "
                }
            }
            structName += ">"
        }
        return structName
    }

    override fun abilities(): Set<Ability> {
        return this.structSignature.abilities.mapNotNull { it.ability }.toSet()
    }

    override fun definingModule(): MoveModuleDef? {
        return this.structSignature.containingModule
    }

    override fun compatibleWith(other: BaseType): Boolean {
        return other is StructType
                && this.structFullname() == other.structFullname()
                && this.typeArgumentTypes.size == other.typeArgumentTypes.size
                && this.typeArgumentTypes.zip(other.typeArgumentTypes)
            .all { (left, right) ->
                left == null || right == null
                        || left.compatibleWith(right)
            }
    }

    fun structDef(): MoveStructDef? {
        return this.structSignature.structDef
    }

    fun structFullname(): String {
        val moduleName = structSignature.containingModule?.name ?: return this.name()
        val address = structSignature.containingAddress.text
        return "$address::$moduleName::${this.name()}"
    }

    fun typeVars(): TypeVarsMap {
        val typeParams = this.structSignature.typeParameters
        if (typeParams.size != this.typeArgumentTypes.size) return emptyMap()

        val typeVars = mutableMapOf<String, BaseType?>()
        for (i in typeParams.indices) {
            val name = typeParams[i].name ?: continue
            val type = typeArgumentTypes[i]
            typeVars[name] = type
        }
        return typeVars
    }
}

class TypeParamType(private val typeParam: MoveTypeParameter) : BaseType() {

    override fun name(): String = typeParam.name ?: ""
    override fun fullname(): String = this.name()
    override fun definingModule(): MoveModuleDef? = null

    override fun abilities(): Set<Ability> {
        return typeParam.abilities.mapNotNull { it.ability }.toSet()
    }

    override fun compatibleWith(other: BaseType): Boolean {
        return (this.abilities() - other.abilities()).isEmpty()
    }

    companion object {
        fun withSubstitutedTypeVars(
            typeParam: MoveTypeParameter,
            typeVars: TypeVarsMap
        ): BaseType? {
            val name = typeParam.name ?: return TypeParamType(typeParam)
            return typeVars.getOrDefault(name, TypeParamType(typeParam))
        }
    }
}

//fun isCompatibleTypes(expectedType: BaseType, actualType: BaseType): Boolean {
//    if (expectedType::class != actualType::class) return false
//    when {
//        expectedType is RefType && actualType is RefType -> {
//            if (expectedType.fullname() == actualType.fullname()) return true
//
//            val refsCompatible =
//                isCompatibleTypes(
//                    expectedType.referredType,
//                    actualType.referredType
//                )
//            return refsCompatible && (!expectedType.mutable && actualType.mutable)
//        }
//    }
//    return expectedType::class == actualType::class
//            && expectedType.fullname() == actualType.fullname()
//}
