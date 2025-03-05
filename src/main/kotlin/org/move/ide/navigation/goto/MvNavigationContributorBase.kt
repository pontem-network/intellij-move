package org.move.ide.navigation.goto

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.GotoClassContributor
import com.intellij.navigation.NavigationItem
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.types.fqName

abstract class MvNavigationContributorBase: ChooseByNameContributorEx,
                                            GotoClassContributor {

    override fun getQualifiedName(item: NavigationItem): String? =
        (item as? MvNamedElement)?.fqName()?.identifierText()

    override fun getQualifiedNameSeparator(): String = "::"
}