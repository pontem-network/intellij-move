package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvStructBlock
import org.move.lang.core.psi.MvStructField

val MvStructField.fieldsDefBlock: MvStructBlock?
    get() =
        parent as? MvStructBlock

val MvStructField.structItem: MvStruct
    get() =
        fieldsDefBlock?.parent as MvStruct
