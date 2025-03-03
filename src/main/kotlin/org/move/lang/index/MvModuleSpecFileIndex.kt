package org.move.lang.index

import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.move.lang.MoveFile
import org.move.lang.MoveFileType
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvModuleSpec
import org.move.lang.core.psi.ext.moduleItem
import org.move.lang.core.types.fqName
import org.move.lang.moveProject
import org.move.lang.toMoveFile

class MvModuleSpecFileIndex: MvScalarFileIndexExtension() {
    override fun getName(): ID<String, Void> = INDEX_ID

    override fun getIndexer(): DataIndexer<String, Void?, FileContent> {
        return object: DataIndexer<String, Void?, FileContent> {
            override fun map(inputData: FileContent): Map<String, Void?> {
                val file = inputData.psiFile as MoveFile
                // build a list of all modules for which there is module specs
                val pathModuleIds = file.moduleSpecs().mapNotNull { it.path?.text }
                return pathModuleIds.associate { it to null }
            }
        }
    }

    @Suppress("CompanionObjectInExtension")
    companion object {
        val INDEX_ID: ID<String, Void> = ID.create("org.move.index.MvModuleSpecFileIndex")

        fun getSpecsForModule(module: MvModule): List<MvModuleSpec> {
            val project = module.project

            val moduleFqName = module.fqName() ?: return emptyList()
            val searchScope = module.moveProject?.searchScope() ?: return emptyList()

            // need to cover all possibilities, as index is a path text
            val indexIds = hashSetOf(
                moduleFqName.declarationText(),
                moduleFqName.shortAddressValueText(),
                moduleFqName.canonicalAddressValueText(),
                moduleFqName.universalAddressText(),
            )
            val filesIndex = FileBasedIndex.getInstance()
            return buildList {
                val vFiles =
                    filesIndex.getContainingFilesForAnyKey(INDEX_ID, indexIds, searchScope)
                val files = vFiles.mapNotNull { it.toMoveFile(project) }
                for (file in files) {
                    for (moduleSpec in file.moduleSpecs()) {
                        // hits name resolution here
                        if (moduleSpec.moduleItem == module) {
                            add(moduleSpec)
                        }
                    }
                }
            }
        }
    }
}

