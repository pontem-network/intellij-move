package org.move.lang.core.stubs

import com.intellij.psi.stubs.IndexSink
import org.move.lang.core.psi.MvFunction
import org.move.lang.index.MvEntryFunctionIndex
import org.move.lang.index.MvModuleSpecIndex
import org.move.lang.index.MvNamedElementIndex
import org.move.lang.index.MvViewFunctionIndex
import org.move.lang.moveProject

fun IndexSink.indexModuleStub(stub: MvModuleStub) {
    indexNamedStub(stub)
}

fun IndexSink.indexFunctionStub(stub: MvFunctionStub) {
    indexNamedStub(stub)
    if (!stub.isTest) {
        stub.unresolvedQualName?.let {
            when {
                stub.isEntry -> occurrence(MvEntryFunctionIndex.KEY, it)
                stub.isView -> occurrence(MvViewFunctionIndex.KEY, it)
            }
        }
        val function = stub.psi as MvFunction
        function.moveProject?.let { proj ->
            stub.resolvedQualName(proj)
                ?.let {
                    when {
                        stub.isEntry -> occurrence(MvEntryFunctionIndex.KEY, it)
                        stub.isView -> occurrence(MvViewFunctionIndex.KEY, it)
                    }
                }
        }
    }
}

fun IndexSink.indexSpecFunctionStub(stub: MvSpecFunctionStub) {
    indexNamedStub(stub)
}

fun IndexSink.indexStructStub(stub: MvStructStub) {
    indexNamedStub(stub)
}

fun IndexSink.indexSchemaStub(stub: MvSchemaStub) {
    indexNamedStub(stub)
}

fun IndexSink.indexConstStub(stub: MvConstStub) {
    indexNamedStub(stub)
}

fun IndexSink.indexModuleSpecStub(stub: MvModuleSpecStub) {
    stub.moduleName?.let {
        occurrence(MvModuleSpecIndex.KEY, it)
    }
}

private fun IndexSink.indexNamedStub(stub: MvNamedStub) {
    stub.name?.let {
        occurrence(MvNamedElementIndex.KEY, it)
    }
}
