package org.move.lang.core.stubs.index

//import com.intellij.psi.stubs.IndexSink
//import com.intellij.psi.stubs.StringStubIndexExtension
//import com.intellij.psi.stubs.StubIndexKey
//import org.move.lang.core.psi.MoveModuleDef
//import org.move.lang.core.stubs.MoveFileStub
//import org.move.lang.core.stubs.impl.MoveModuleDefStub
//
//class MoveModulesIndex : StringStubIndexExtension<MoveModuleDef>() {
//    override fun getVersion(): Int = MoveFileStub.Type.stubVersion
//    override fun getKey(): StubIndexKey<String, MoveModuleDef> = KEY
//
//    companion object {
//        val KEY: StubIndexKey<String, MoveModuleDef> =
//            StubIndexKey.createIndexKey("org.move.lang.core.stubs.index.MoveModulesIndex")
//
//        fun index(stub: MoveModuleDefStub, indexSink: IndexSink) {
//            val address = stub.address
//            val name = stub.name
//            if (name != null) {
//                indexSink.occurrence(KEY, address + "_" + name)
//            }
//        }
//    }
//
//}