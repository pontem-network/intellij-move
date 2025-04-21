package org.move.ide.search

import com.intellij.psi.PsiElement
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProviderEx
import org.move.lang.core.psi.MvNamedAddress

class NamedAddressUsageTypeProvider: UsageTypeProviderEx {
    override fun getUsageType(element: PsiElement, targets: Array<out UsageTarget>): UsageType? {
        return when (element) {
            is MvNamedAddress -> UsageType { "address ref" }
            else -> null
        }
    }

    override fun getUsageType(element: PsiElement): UsageType? {
        return getUsageType(element, UsageTarget.EMPTY_ARRAY)
    }
}
