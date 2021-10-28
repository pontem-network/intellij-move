package org.move.lang.core.psi.mixins

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.util.PlatformIcons
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveFunctionSignature
import org.move.lang.core.psi.MoveNativeFunctionDef
import org.move.lang.core.psi.ext.parametersText
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.VoidType
import javax.swing.Icon

val MoveFunctionSignature.isNative get() = this.parent is MoveNativeFunctionDef

val MoveFunctionSignature.resolvedReturnType: BaseType?
    get() {
        val returnTypeElement = this.returnType
        return if (returnTypeElement == null) {
            VoidType()
        } else {
            returnTypeElement.type?.resolvedType(emptyMap())
        }
    }

abstract class MoveFunctionSignatureMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                           MoveFunctionSignature {
    var builtIn = false

    override fun canNavigate(): Boolean = !builtIn
    override fun canNavigateToSource(): Boolean = !builtIn

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION

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
