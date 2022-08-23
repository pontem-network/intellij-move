package org.move.lang.index

import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.move.lang.MoveFileType

abstract class BaseMoveFileIndex: ScalarIndexExtension<String>() {
    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE
    override fun dependsOnFileContent() = true
    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(MoveFileType)

    companion object {
        fun requestRebuildIndices() {
            MvModuleSpecIndex.requestRebuild()
            MvNamedElementIndex.requestRebuild()
        }
    }
}
