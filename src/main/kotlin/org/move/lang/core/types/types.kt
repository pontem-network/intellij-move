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

    fun label(): String = this.name.lowercase()

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
    abstract fun compatibleWith(actualType: BaseType): Boolean

    open fun typeLabel(relativeTo: MoveElement): String {
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

open class PrimitiveType(private val name: String) : BaseType() {
    override fun name(): String = name
    override fun fullname(): String = name()
    override fun abilities(): Set<Ability> = setOf(Ability.DROP, Ability.COPY, Ability.STORE)
    override fun definingModule(): MoveModuleDef? = null

    override fun compatibleWith(actualType: BaseType): Boolean {
        // &address is compatible with address
//        if (this.isCopyable()
//            && other is RefType
//            && other.innerReferredType() is PrimitiveType
//            && (other.innerReferredType() as PrimitiveType).isCopyable()
//        ) return true

        return actualType is PrimitiveType && this.name == actualType.name()
    }
}

class SignerType : BaseType() {
    override fun name(): String = "signer"
    override fun fullname(): String = "signer"
    override fun abilities(): Set<Ability> = setOf(Ability.DROP)
    override fun definingModule(): MoveModuleDef? = null

    override fun compatibleWith(actualType: BaseType): Boolean {
        return actualType is SignerType
    }
}

class IntegerType(
    val precision: String? = null
) :
    PrimitiveType(precision ?: "integer") {

    override fun name(): String = precision ?: "integer"
    override fun fullname(): String = name()
    override fun abilities(): Set<Ability> = Ability.all()
    override fun definingModule(): MoveModuleDef? = null

    override fun compatibleWith(actualType: BaseType): Boolean {
        if (actualType is TypeParamType) return true
        if (actualType !is IntegerType) return false
        return this.precision == null
                || actualType.precision == null
                || this.precision == actualType.precision
    }
}

class VectorType(private val itemType: BaseType) : BaseType() {
    override fun name(): String = "vector<${itemType.fullname()}>"
    override fun fullname(): String = name()
    override fun abilities(): Set<Ability> = itemType.abilities()
    override fun definingModule(): MoveModuleDef? = null

    override fun compatibleWith(actualType: BaseType): Boolean {
        if (actualType is TypeParamType) return true
        return actualType is VectorType
                && this.itemType.compatibleWith(actualType.itemType)
    }
}

class VoidType : BaseType() {
    override fun name(): String = "()"
    override fun fullname(): String = "()"
    override fun abilities(): Set<Ability> = emptySet()
    override fun definingModule(): MoveModuleDef? = null

    override fun compatibleWith(actualType: BaseType): Boolean {
        return actualType is VoidType
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
    override fun abilities(): Set<Ability> = setOf(Ability.COPY, Ability.DROP)
    override fun definingModule(): MoveModuleDef? = referredType.definingModule()

    override fun compatibleWith(actualType: BaseType): Boolean {
        if (actualType is TypeParamType) return true
        if (actualType !is RefType) return false
        if (this.fullname() == actualType.fullname()) return true

        return this.referredType.compatibleWith(actualType.referredType)
                && (!this.mutable || actualType.mutable)
    }

    override fun typeLabel(relativeTo: MoveElement): String {
        return prefix() + referredType.typeLabel(relativeTo)
    }

//    fun referredTypeName(): String = referredType.name()
//    fun referredTypeFullName(): String = referredType.fullname()

    fun referredStructDef(): MoveStructDef? =
        when (referredType) {
            is StructType -> referredType.structDef()
            is RefType -> referredType.referredStructDef()
            else -> null
        }

    fun innerReferredType(): BaseType {
        var referredType = this.referredType
        while (referredType is RefType) {
            referredType = referredType.referredType;
        }
        return referredType
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

    override fun compatibleWith(actualType: BaseType): Boolean {
        if (actualType is TypeParamType) return true
        return actualType is StructType
                && this.structFullname() == actualType.structFullname()
                && this.typeArgumentTypes.size == actualType.typeArgumentTypes.size
                && this.typeArgumentTypes.zip(actualType.typeArgumentTypes)
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

    override fun typeLabel(relativeTo: MoveElement): String {
        val typeLabel = super.typeLabel(relativeTo)
        return typeLabel + typeArgumentsLabel(relativeTo)
    }

    private fun typeArgumentsLabel(relativeTo: MoveElement): String {
        var label = ""
        if (this.typeArgumentTypes.isNotEmpty()) {
            label += "<"
            for ((i, typeArgumentType) in typeArgumentTypes.withIndex()) {
                label += typeArgumentType?.typeLabel(relativeTo) ?: "unknown_type"
                if (i < typeArgumentTypes.size - 1) {
                    label += ", "
                }
            }
            label += ">"
        }
        return label
    }
}

class TypeParamType(private val typeParam: MoveTypeParameter) : BaseType() {

    override fun name(): String = typeParam.name ?: ""
    override fun fullname(): String = this.name()
    override fun definingModule(): MoveModuleDef? = null

    override fun abilities(): Set<Ability> {
        return typeParam.abilities.mapNotNull { it.ability }.toSet()
    }

    override fun compatibleWith(actualType: BaseType): Boolean {
        if (actualType is TypeParamType) return true
        return (this.abilities() - actualType.abilities()).isEmpty()
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
