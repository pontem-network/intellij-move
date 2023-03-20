package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvSchema
import org.move.lang.core.psi.MvSchemaLitExpr
import org.move.lang.core.psi.MvSchemaLitField

val MvSchemaLitExpr.schema: MvSchema? get() = this.path.reference?.resolveWithAliases() as? MvSchema

val MvSchemaLitExpr.fields: List<MvSchemaLitField> get() = schemaFieldsBlock.schemaLitFieldList

val MvSchemaLitExpr.fieldNames: List<String> get() = fields.map { it.referenceName }
