package org.move.lang.core.stubs

import com.intellij.psi.stubs.IndexSink
import org.move.lang.core.stubs.index.MoveNamedElementIndex

fun IndexSink.indexFunctionDef(stub: MoveFunctionDefStub) {
    indexNamedStub(stub)
}

private fun IndexSink.indexNamedStub(stub: MoveNamedStub) {
    stub.name?.let {
        occurrence(MoveNamedElementIndex.KEY, it)
    }
}