package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

//abstract class MovePatMixin(node: ASTNode) : MoveElementImpl(node), MovePat {
//    override fun resolvePatternType(parentPatternType: BaseType): BaseType? {
//        return null
//    }
//}

//fun resolvePatternBindingType(bindingPat: MoveBindingPat, patternType: BaseType): BaseType? {
//    val parentPattern = bindingPat.ancestorStrict<MovePat>()
//    if (parentPattern == null) return patternType
//
//    return when (parentPattern) {
//        is MoveStructPat -> {
//            for (field in parentPattern.providedFields) {
//                val fieldBinding = field.structPatFieldBinding
//                if (fieldBinding == null) continue
//                if (fieldBinding.pat == bindingPat) {
//                    val referredField = field.reference?.resolve() as? MoveStructFieldDef
//                    if (referredField == null) continue
//                    return referredField.resolvedType()
//                }
//            }
//            return null
//        }
//        else -> null
//    }
//    var bindingType = patternType;
//    while (parentPattern != null) {
//        when (parentPattern) {
//            is MoveTuplePat -> {
//                val bindingIndex = parentPattern.patList.indexOf(bindingPat)
//                bindingPat
//            }
//        }
//    }
//
//    return bindingType
//}

val MovePat.boundElements: List<MoveNamedElement>
    get() {
        val elements = mutableListOf<MoveNamedElement>()
        accept(object : MoveVisitor() {
            override fun visitBindingPat(o: MoveBindingPat) {
                elements.add(o)
            }

            override fun visitStructPat(o: MoveStructPat) {
                o.structPatFieldsBlock.structPatFieldList.forEach { field ->
                    field.structPatFieldBinding?.pat?.accept(this) ?: elements.add(field)
                }
            }

            override fun visitTuplePat(o: MoveTuplePat) {
                o.patList.forEach { it.accept(this) }
            }
        })
        return elements
    }
