package org.move.ide.utils.imports

import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.types.ItemFQName

data class ImportCandidate(val element: MvNamedElement, val qualName: ItemFQName)
