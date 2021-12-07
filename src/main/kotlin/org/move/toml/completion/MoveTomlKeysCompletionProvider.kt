package org.move.toml.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.toml.isAddressesListHeader
import org.move.toml.isDependencyListHeader
import org.toml.lang.psi.*

class MoveTomlKeysCompletionProvider: CompletionProvider<CompletionParameters>() {
    private var cachedSchema: TomlSchema? = null

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val schema = cachedSchema
            ?: TomlSchema.parse(parameters.position.project, EXAMPLE_MOVE_TOML).also { cachedSchema = it }

        val key = parameters.position.parent as? TomlKeySegment ?: return
        val table = key.topLevelTable ?: return
        val variants = when (val parent = key.parent?.parent) {
            is TomlTableHeader -> {
                if (key != parent.key?.segments?.firstOrNull()) return
                val isArray = when (table) {
                    is TomlArrayTable -> true
                    is TomlTable -> false
                    else -> return
                }
                schema.topLevelKeys(isArray)
            }
            is TomlKeyValue -> {
                if (table !is TomlHeaderOwner) return
                if (table.header.isDependencyListHeader || table.header.isAddressesListHeader) return
                schema.keysForTable(table.name ?: return)
            }
            else -> return
        }

        result.addAllElements(variants.map {
            LookupElementBuilder.create(it)
        })
    }
}

private val TomlKeySegment.topLevelTable: TomlKeyValueOwner?
    get() {
        val table = ancestorStrict<TomlKeyValueOwner>() ?: return null
        if (table.parent !is TomlFile) return null
        return table
    }

private val TomlHeaderOwner.name: String?
    get() = header.key?.segments?.firstOrNull()?.name

@Language("TOML")
private val EXAMPLE_MOVE_TOML = """
   
[package]
name = "package"
version = "0.1.0"
license = "MIT"
authors = []

[addresses]
Std = "_"

[dependencies]
MoveStdlib = { local = "<some_path>/move-stdlib", addr_subst = { "Std" = "0x1" } }

[dev-addresses]
Std = "0x1"
"""
