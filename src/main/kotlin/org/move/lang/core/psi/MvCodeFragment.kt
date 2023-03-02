package org.move.lang.core.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.LightVirtualFile
import org.move.lang.MoveFileBase
import org.move.lang.MoveFileType
import org.move.lang.MoveLanguage

abstract class MvCodeFragment(
    fileViewProvider: FileViewProvider,
    contentElementType: IElementType,
    val context: MvElement,
    forceCachedPsi: Boolean = true,
) : MoveFileBase(fileViewProvider), PsiCodeFragment {

    constructor(
        project: Project,
        text: CharSequence,
        contentElementType: IElementType,
        context: MvElement
    ) : this(
        PsiManagerEx.getInstanceEx(project).fileManager.createFileViewProvider(
            LightVirtualFile("fragment.move", MoveLanguage, text), true
        ),
        contentElementType,
        context
    )

    override fun accept(visitor: PsiElementVisitor) {
        visitor.visitFile(this)
    }

    override fun getFileType(): FileType = MoveFileType

    private var viewProvider = super.getViewProvider() as SingleRootFileViewProvider
    private var forcedResolveScope: GlobalSearchScope? = null
    private var isPhysical = true

    init {
        if (forceCachedPsi) {
            getViewProvider().forceCachedPsi(this)
        }
        init(TokenType.CODE_FRAGMENT, contentElementType)
    }

    final override fun init(elementType: IElementType, contentElementType: IElementType?) {
        super.init(elementType, contentElementType)
    }

    override fun isPhysical() = isPhysical

    override fun forceResolveScope(scope: GlobalSearchScope?) {
        forcedResolveScope = scope
    }

    override fun getForcedResolveScope(): GlobalSearchScope? = forcedResolveScope

    override fun getContext(): PsiElement = context

    final override fun getViewProvider(): SingleRootFileViewProvider = viewProvider

    override fun isValid() = true

    override fun clone(): PsiFileImpl {
        val clone = cloneImpl(calcTreeElement().clone() as FileElement) as MvCodeFragment
        clone.isPhysical = false
        clone.myOriginalFile = this
        clone.viewProvider =
            SingleRootFileViewProvider(
                PsiManager.getInstance(project),
                LightVirtualFile(name, MoveLanguage, text),
                false
            )
        clone.viewProvider.forceCachedPsi(clone)
        return clone
    }
}

class MvQualPathTypeCodeFragment(
    project: Project,
    text: CharSequence,
    context: MvElement
) : MvCodeFragment(project, text, MvCodeFragmentElementType.QUAL_PATH_TYPE, context)
