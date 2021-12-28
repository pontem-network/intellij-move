package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.util.PlatformIcons
import org.move.ide.MvIcons
import org.move.ide.annotator.BUILTIN_FUNCTIONS
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnit
import org.move.lang.core.types.ty.TyUnknown
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
        return when (visibility.node.text) {
            "public" -> FunctionVisibility.PUBLIC
            "public(friend)" -> FunctionVisibility.PUBLIC_FRIEND
            "public(script)" -> FunctionVisibility.PUBLIC_SCRIPT
            else -> FunctionVisibility.PRIVATE
        }
    }

val MvFunction.module: MvModuleDef?
    get() {
        val moduleBlock = this.parent
        return moduleBlock.parent as? MvModuleDef
    }

val MvFunction.script: MvScriptDef?
    get() {
        val scriptBlock = this.parent
        return scriptBlock.parent as? MvScriptDef
    }

val MvFunction.isTest: Boolean get() = this.attrList.findSingleItemAttr("test") != null

val MvFunction.isNative get() = hasChild(MvElementTypes.NATIVE)

val MvFunction.isBuiltinFunc get() = this.isNative && this.name in BUILTIN_FUNCTIONS

val MvFunction.parameters get() = this.functionParameterList?.functionParameterList.orEmpty()

val MvFunction.typeParameters get() = this.typeParameterList?.typeParameterList.orEmpty()

val MvFunction.acquiresPathTypes: List<MvPathType> get() = this.acquiresType?.pathTypeList.orEmpty()

val MvFunction.resolvedReturnTy: Ty
    get() {
        val returnTypeElement = this.returnType
        return if (returnTypeElement == null) {
            TyUnit
        } else {
            returnTypeElement.type?.inferTypeTy() ?: TyUnknown
        }
    }

abstract class MvFunctionMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                MvFunction {
    var builtIn = false

    override fun canNavigate(): Boolean = !builtIn
    override fun canNavigateToSource(): Boolean = !builtIn

    override fun getIcon(flags: Int): Icon = MvIcons.FUNCTION

    override fun getPresentation(): ItemPresentation? {
        val name = this.name ?: return null
        val params = this.functionParameterList?.parametersText ?: "()"
        val returnTypeText = this.returnType?.type?.text ?: ""
        val tail = if (returnTypeText == "") "" else ": $returnTypeText"
        return PresentationData(
            "$name$params$tail",
            null,
            PlatformIcons.PUBLIC_ICON,
            TextAttributesKey.createTextAttributesKey("public")
        );
    }
}
