package org.move.lang.index

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.stubs.impl.MvFileStub

class MvFQFunctionIndex : StringStubIndexExtension<MvFunction>() {
    override fun getKey() = KEY
    override fun getVersion(): Int = MvFileStub.Type.stubVersion

    companion object {
        val KEY: StubIndexKey<String, MvFunction> =
            StubIndexKey.createIndexKey("org.move.index.FQFunctionIndex")

//        fun getFunctionByFQName(fqName: String): MvFunction? {
//            val parts = fqName.split("::")
//
//            val address = parts.getOrNull(0) ?: return null
//            val module = parts.getOrNull(1) ?: return null
//            val name = parts.getOrNull(2) ?: return null
//
//
//        }
    }
}
