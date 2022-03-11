package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

val MvSchemaLit.schema: MvSchema? get() = this.path.reference?.resolve() as? MvSchema

val MvSchemaLit.fields: List<MvSchemaLitField>
    get() =
        schemaFieldsBlock?.schemaLitFieldList.orEmpty()

val MvSchemaLit.fieldNames: List<String>
    get() =
        fields.map { it.referenceName }
