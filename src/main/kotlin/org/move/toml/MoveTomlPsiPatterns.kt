package org.move.toml

import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.VirtualFilePattern
import com.intellij.psi.PsiElement
import org.move.cli.MoveConstants
import org.move.lang.core.psiElement
import org.move.lang.core.withCond
import org.toml.lang.psi.*
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind
import org.toml.lang.psi.ext.name

object MoveTomlPsiPatterns {
    private inline fun <reified I : PsiElement> moveTomlPsiElement(): PsiElementPattern.Capture<I> {
        return psiElement<I>().inVirtualFile(
            VirtualFilePattern().withName(MoveConstants.MANIFEST_FILE))
    }

    /** Any element inside any TomlKey in Move.toml */
    val inKey: PsiElementPattern.Capture<PsiElement> =
        moveTomlPsiElement<PsiElement>()
            .withParent(TomlKeySegment::class.java)

    fun dependencyProperty(name: String): PsiElementPattern.Capture<TomlKeyValue> = psiElement<TomlKeyValue>()
        .withCond("name") { e -> e.key.name == name }
        .withParent(
            psiElement<TomlInlineTable>().withSuperParent(2, onDependencyTable())
        )

    fun dependencyLocal(): PsiElementPattern.Capture<TomlLiteral> = moveTomlStringLiteral()
        .withParent(dependencyProperty("local"))

    fun dependencyGitUrl(): PsiElementPattern.Capture<TomlLiteral> = moveTomlStringLiteral()
        .withParent(dependencyProperty("git"))

    private fun onDependencyTableHeader(): PsiElementPattern.Capture<TomlTableHeader> =
        moveTomlPsiElement<TomlTableHeader>()
            .withCond("dependenciesCondition") { header ->
                header.isDependencyListHeader
            }

    private fun onDependencyTable(): PsiElementPattern.Capture<TomlTable> =
        moveTomlPsiElement<TomlTable>()
            .withChild(onDependencyTableHeader())

    private fun moveTomlStringLiteral() = moveTomlPsiElement<TomlLiteral>()
        .withCond("stringLiteral") { e -> e.kind is TomlLiteralKind.String }
}
