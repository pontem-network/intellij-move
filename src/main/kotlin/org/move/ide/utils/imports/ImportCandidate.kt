package org.move.ide.utils.imports

import org.move.lang.core.psi.MvQualNamedElement
import org.move.lang.core.types.ItemQualName

data class ImportCandidate(val element: MvQualNamedElement, val qualName: ItemQualName)
