package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.MoveStructFieldDef
import org.move.lang.core.psi.MoveStructFieldsDefBlock

val MoveStructFieldDef.fieldsDefBlock: MoveStructFieldsDefBlock?
    get() =
        parent as? MoveStructFieldsDefBlock

val MoveStructFieldDef.structDef: MoveStructDef?
    get() =
        fieldsDefBlock?.parent as? MoveStructDef
