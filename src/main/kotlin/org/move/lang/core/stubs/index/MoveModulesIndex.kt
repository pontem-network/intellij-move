package org.move.lang.core.stubs.index

//import com.intellij.psi.stubs.IndexSink
//import com.intellij.psi.stubs.StringStubIndexExtension
//import com.intellij.psi.stubs.StubIndexKey
//import org.move.lang.core.psi.MvModuleDef
//import org.move.lang.core.stubs.MvFileStub
//import org.move.lang.core.stubs.impl.MvModuleDefStub
//
//class MvModulesIndex : StringStubIndexExtension<MvModuleDef>() {
//    override fun getVersion(): Int = MvFileStub.Type.stubVersion
//    override fun getKey(): StubIndexKey<String, MvModuleDef> = KEY
//
//    companion object {
//        val KEY: StubIndexKey<String, MvModuleDef> =
//            StubIndexKey.createIndexKey("org.move.lang.core.stubs.index.MvModulesIndex")
//
//        fun index(stub: MvModuleDefStub, indexSink: IndexSink) {
//            val address = stub.address
//            val name = stub.name
//            if (name != null) {
//                indexSink.occurrence(KEY, address + "_" + name)
//            }
//        }
//    }
//
//}
