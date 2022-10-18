package org.move.toml.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.ancestors
import org.toml.lang.psi.*

private fun CompletionResultSet.addNames(names: Collection<String>) {
    this.addAllElements(names.map {
        LookupElementBuilder.create(it)
    })
}

class MoveTomlKeysCompletionProvider : CompletionProvider<CompletionParameters>() {
    private var cachedSchema: MoveTomlSchema? = null

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val schema = cachedSchema
            ?: MoveTomlSchema.parse(parameters.position.project, EXAMPLE_MOVE_TOML).also { cachedSchema = it }

        val key = parameters.position.parent as? TomlKeySegment ?: return
        val topTable = key.topTable()
        // top level keys
        if (topTable == null) return

        val header = key.ancestorStrict<TomlTableHeader>()
        if (header != null && topTable.header == header) {
            // header of the top table
            result.addNames(schema.tableHeaderKeys())
            return
        }

        val topTableName = topTable.header.key?.segments?.firstOrNull()?.text ?: return
        if (topTableName == "package") {
            result.addNames(schema.keysForTable("package"))
            return
        }

        if (topTableName.startsWith("dependencies")
            || topTableName.startsWith("dev_dependencies")
        ) {
            // dep keys
            val inlineTable = key.ancestorStrict<TomlInlineTable>()
            if (inlineTable != null) {
                // inline table dependency
                val keyValue = inlineTable.parent as? TomlKeyValue ?: return
                if (keyValue.key.segments.firstOrNull()?.text.orEmpty() == "addr_subst") return
                result.addNames(schema.keysForDependency())
            } else {
                // table dependency
                result.addNames(schema.keysForDependency())
            }
        }
    }
}

fun TomlElement.topTable(): TomlTable? =
    this.ancestors.filterIsInstance<TomlTable>().filter { it.parent is TomlFile }.firstOrNull()

@Language("TOML")
private val EXAMPLE_MOVE_TOML = """
   
[package]
name = "package"
version = "0.1.0"
license = "MIT"
authors = []
upgrade_policy = "compatible"

[addresses]
Std = "_"

[dev-addresses]
Std = "0x1"

[dependencies]
[dev-dependencies]
[build]
"""
