package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvSchema
import org.move.lang.core.psi.MvSchemaLit
import org.move.lang.core.psi.MvSchemaLitField

val MvSchemaLit.schema: MvSchema? get() = this.path.reference?.resolveFollowingAliases() as? MvSchema

val MvSchemaLit.fields: List<MvSchemaLitField> get() = schemaFieldsBlock?.schemaLitFieldList.orEmpty()

val MvSchemaLit.fieldNames: List<String> get() = fields.map { it.referenceName }
