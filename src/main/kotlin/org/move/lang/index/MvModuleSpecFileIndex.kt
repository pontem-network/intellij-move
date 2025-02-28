package org.move.lang.index

import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.move.lang.MoveFile
import org.move.lang.MoveFileType
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvModuleSpec
import org.move.lang.core.psi.ext.moduleItem
import org.move.lang.core.types.indexId
import org.move.lang.moveProject
import org.move.lang.toMoveFile

class MvModuleSpecFileIndex: ScalarIndexExtension<String>() {
    override fun getName(): ID<String, Void> = INDEX_ID
    override fun getVersion(): Int = 1
    override fun dependsOnFileContent(): Boolean = true

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(MoveFileType)

    override fun getIndexer(): DataIndexer<String, Void?, FileContent> {
        return object: DataIndexer<String, Void?, FileContent> {
            override fun map(inputData: FileContent): Map<String, Void?> {
                val file = inputData.psiFile as MoveFile
                // build a list of all modules for which there is module specs
                val moduleIds = file.moduleSpecs().mapNotNull { it.moduleItem?.indexId() }
                return moduleIds.associate { it to null }
            }
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String?> = EnumeratorStringDescriptor.INSTANCE

    @Suppress("CompanionObjectInExtension")
    companion object {
        val INDEX_ID: ID<String, Void> = ID.create("org.move.index.MvModuleSpecFileIndex")

        fun getSpecsForModule(module: MvModule): List<MvModuleSpec> {
            val filesIndex = FileBasedIndex.getInstance()
            val project = module.project
            return buildList {
                val searchScope = module.moveProject?.searchScope() ?: return@buildList
                val indexId = module.indexId() ?: return@buildList

                val vFiles = filesIndex.getContainingFiles(INDEX_ID, indexId, searchScope)
                val files = vFiles.mapNotNull { it.toMoveFile(project) }
                for (file in files) {
                    // todo: should I check for validity here?
                    addAll(file.moduleSpecs())
                }
            }


        }
    }
}