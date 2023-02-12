package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvConst
import org.move.lang.core.psi.MvModule
import org.move.lang.core.types.infer.ItemContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

fun MvConst.constAnnotationTy(itemContext: ItemContext): Ty {
    return this.typeAnnotation?.type
        ?.let { itemContext.getTypeTy(it) } ?: TyUnknown
}

val MvConst.module: MvModule?
    get() {
        val moduleBlock = this.parent
        return moduleBlock.parent as? MvModule
    }

//abstract class MvConstMixin: MvElementImpl,
//                             MvConst {
//
//    constructor(node: ASTNode) : super(node)

//    constructor(stub: MvConstStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

//    override val fqName: String
//        get() {
//            val moduleFqName = this.module?.fqName?.let { "$it::" }
//            val name = this.bindingPat?.name ?: "<unknown>"
//            return moduleFqName + name
//        }
//}
