package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import com.intellij.util.PlatformIcons
import org.move.ide.MoveIcons
import org.move.ide.annotator.BUILTIN_FUNCTIONS
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvItemSpec
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.psi.module
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferTypeTy
import org.move.lang.core.types.ty.Ty
import javax.swing.Icon

enum class FunctionVisibility {
    PRIVATE,
    PUBLIC,
    PUBLIC_FRIEND,
    PUBLIC_SCRIPT;
}

val MvFunction.visibility: FunctionVisibility
    get() {
        val visibility = this.functionVisibilityModifier ?: return FunctionVisibility.PRIVATE
        return when {
            visibility.hasChild(MvElementTypes.FRIEND) -> FunctionVisibility.PUBLIC_FRIEND
            visibility.hasChild(MvElementTypes.SCRIPT_KW) -> FunctionVisibility.PUBLIC_SCRIPT
            visibility.hasChild(MvElementTypes.PUBLIC) -> FunctionVisibility.PUBLIC
            else -> FunctionVisibility.PRIVATE
        }
    }

val MvFunction.isEntry: Boolean get() = this.isChildExists(MvElementTypes.ENTRY)

val MvFunction.isTest: Boolean
    get() = getProjectPsiDependentCache(this) {
        it.findSingleItemAttr("test") != null
    }

val MvFunction.acquiresTys: List<Ty>
    get() =
        this.acquiresType?.pathTypeList.orEmpty().map {
            // TODO: should be TypeContext from module (see StructField type checking)
            inferTypeTy(it, InferenceContext(true))
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

abstract class MvFunctionMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                MvFunction {
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
