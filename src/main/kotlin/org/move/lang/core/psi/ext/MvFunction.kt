package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.ide.MoveIcons
import org.move.ide.annotator.BUILTIN_FUNCTIONS
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.stubs.MvFunctionStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnit
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.index.MvModuleSpecFileIndex
import javax.swing.Icon

val MvFunction.isEntry: Boolean
    get() {
        val stub = greenStub
        return stub?.isEntry ?: this.isChildExists(MvElementTypes.ENTRY)
    }

val MvFunction.isPublicScript: Boolean get() = this.visibilityModifier?.hasScript ?: false

val MvFunction.isInline: Boolean get() = this.isChildExists(MvElementTypes.INLINE)

val MvFunction.isView: Boolean
    get() {
        val stub = greenStub
        return stub?.isView ?: queryAttributes.isView
    }

val MvFunctionLike.modifiers: List<String> get() {
    // todo: order of appearance
    val item = this
    return buildList {
        if (item is MvFunction) {
            val vis = item.visibilityModifier
            if (vis != null) {
                add(vis.stubVisKind.keyword)
            }
        }
        if (item is MvFunction && item.isEntry) add("entry")
        if (item.isNative) add("native")
        if (item is MvFunction && item.isInline) add("inline")
    }
}

val MvFunction.testAttrItem: MvAttrItem? get() = queryAttributes.getAttrItem("test")

val MvFunction.hasTestAttr: Boolean
    get() {
        val stub = greenStub
        return stub?.isTest ?: queryAttributes.isTest
    }

val QueryAttributes.isTest: Boolean get() = this.hasAttrItem("test")

val QueryAttributes.isView: Boolean get() = this.hasAttrItem("view")

val MvFunction.outerFileName: String
    get() =
        if (this.name in BUILTIN_FUNCTIONS) {
            "builtins"
        } else {
            this.containingFile?.name.orEmpty()
        }

fun MvFunction.innerItemSpecs(): List<MvItemSpec> {
    val functionName = this.name ?: return emptyList()
    val itemSpecs = this.module?.itemSpecList.orEmpty()
    return itemSpecs
        .filter { it.itemSpecRef?.referenceName == functionName }
}

fun MvFunction.outerItemSpecs(): List<MvItemSpec> {
    val functionName = this.name ?: return emptyList()
    val module = this.module ?: return emptyList()
    val moduleSpecs = module.getModuleSpecsFromIndex()
    return moduleSpecs
        .flatMap { it.moduleSpecBlock?.itemSpecList.orEmpty() }
        .filter { it.itemSpecRef?.referenceName == functionName }
}

val MvFunction.transactionParameters: List<MvFunctionParameter> get() = this.parameters.drop(1)

fun MvFunctionLike.returnTypeTy(msl: Boolean): Ty {
    val retType = this.returnType ?: return TyUnit
    return retType.type?.loweredType(msl) ?: TyUnknown
}

val MvFunction.specFunctionResultParameters: List<MvFunctionParameter>
    get() {
        return getProjectPsiDependentCache(this) {
            val retType = it.returnType
            val psiFactory = it.project.psiFactory
            if (retType == null) {
                emptyList()
            } else {
                val retTypeType = retType.type
                when (retTypeType) {
                    null -> emptyList()
                    is MvTupleType -> {
                        retTypeType.typeList
                            .mapIndexed { i, type ->
                                psiFactory.specFunctionParameter(it, "result_${i + 1}", type.text)
                            }
                    }
                    else -> {
                        listOf(
                            psiFactory.specFunctionParameter(it, "result", retTypeType.text)
                        )
                    }
                }
            }
        }
    }

abstract class MvFunctionMixin : MvStubbedNamedElementImpl<MvFunctionStub>,
                                 MvFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: MvFunctionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    var builtIn = false

    override val modificationTracker = MvModificationTracker(this)

    override fun incModificationCount(element: PsiElement): Boolean {
        val shouldInc = codeBlock?.isAncestorOf(element) == true
//        item inside the function
//                && PsiTreeUtil.findChildOfAnyType(
//                    element,
//                    false,
//                    MvNamedElement::class.java,
//                ) == null
        if (shouldInc) modificationTracker.incModificationCount()
        return shouldInc
    }

    override fun canNavigate(): Boolean = !builtIn
    override fun canNavigateToSource(): Boolean = !builtIn

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION
}
