package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.stubs.IStubElementType
import com.intellij.util.PlatformIcons
import org.move.ide.MoveIcons
import org.move.ide.annotator.BUILTIN_FUNCTIONS
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvAttrItem
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvItemSpec
import org.move.lang.core.psi.module
import org.move.lang.core.stubs.MvFunctionStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.infer.ItemContext
import org.move.lang.core.types.ty.Ty
import javax.swing.Icon

enum class FunctionVisibility {
    PRIVATE,
    PUBLIC,
    PUBLIC_FRIEND,
    PUBLIC_SCRIPT;

    companion object {
        fun fromInt(ordinal: Int): FunctionVisibility {
            for (value in FunctionVisibility.values()) {
                if (value.ordinal == ordinal) return value
            }
            error("Invalid value")
        }
    }
}

val MvFunction.visibility: FunctionVisibility
    get() {
        val stub = greenStub
        return stub?.visibility ?: visibilityFromPsi()
    }

fun MvFunction.visibilityFromPsi(): FunctionVisibility {
    val visibility = this.functionVisibilityModifier ?: return FunctionVisibility.PRIVATE
    return when {
        visibility.hasChild(MvElementTypes.FRIEND) -> FunctionVisibility.PUBLIC_FRIEND
        visibility.hasChild(MvElementTypes.SCRIPT_KW) -> FunctionVisibility.PUBLIC_SCRIPT
        visibility.hasChild(MvElementTypes.PUBLIC) -> FunctionVisibility.PUBLIC
        else -> FunctionVisibility.PRIVATE
    }
}

val MvFunction.isEntry: Boolean get() = this.isChildExists(MvElementTypes.ENTRY)

val MvFunction.testAttrItem: MvAttrItem? get() = queryAttributes.getAttrItem("test")

val MvFunction.isTest: Boolean
    get() {
        val stub = greenStub
        return stub?.isTest
            ?: (queryAttributes.isTest)
    }

val QueryAttributes.isTest: Boolean get() = this.hasAttrItem("test")

fun MvFunction.getAcquiresTys(itemContext: ItemContext): List<Ty> =
    this.acquiresType?.pathTypeList.orEmpty().map {
        itemContext.getTypeTy(it)
    }

val MvFunction.signatureText: String
    get() {
        val params = this.functionParameterList?.parametersText ?: "()"
        val returnTypeText = this.returnType?.type?.text ?: ""
        val returnType = if (returnTypeText == "") "" else ": $returnTypeText"
        return "$params$returnType"
    }

val MvFunction.outerFileName: String
    get() =
        if (this.name in BUILTIN_FUNCTIONS) {
            "builtins"
        } else {
            this.containingFile?.name.orEmpty()
        }

fun MvFunction.innerItemSpecs(): List<MvItemSpec> {
    val functionName = this.name ?: return emptyList()
    val itemSpecs = this.module?.moduleBlock?.itemSpecs().orEmpty()
    return itemSpecs
        .filter { it.itemSpecRef?.referenceName == functionName }
}

fun MvFunction.outerItemSpecs(): List<MvItemSpec> {
    val functionName = this.name ?: return emptyList()
    val moduleSpecs = this.module?.allModuleSpecs().orEmpty()
    return moduleSpecs
        .flatMap { it.moduleSpecBlock?.itemSpecs().orEmpty() }
        .filter { it.itemSpecRef?.referenceName == functionName }
}

abstract class MvFunctionMixin : MvStubbedNamedElementImpl<MvFunctionStub>,
                                 MvFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: MvFunctionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    var builtIn = false

    override val fqName: String
        get() {
            val moduleFqName = this.module?.fqName?.let { "$it::" }
            val name = this.name ?: "<unknown>"
            return moduleFqName + name
        }

    override fun canNavigate(): Boolean = !builtIn
    override fun canNavigateToSource(): Boolean = !builtIn

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION

    override fun getPresentation(): ItemPresentation? {
        val name = this.name ?: return null
//        val params = this.functionParameterList?.parametersText ?: "()"
//        val returnTypeText = this.returnType?.type?.text ?: ""
//        val tail = if (returnTypeText == "") "" else ": $returnTypeText"
        return PresentationData(
            "$name${this.signatureText}",
            null,
            PlatformIcons.PUBLIC_ICON,
            TextAttributesKey.createTextAttributesKey("public")
        )
    }
}
