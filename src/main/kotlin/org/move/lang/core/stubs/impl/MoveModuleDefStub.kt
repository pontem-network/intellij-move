package org.move.lang.core.stubs.impl

//class MvModuleDefStub(
//    parent: StubElement<*>?,
//    elementType: IStubElementType<*, *>,
//    val address: String,
//    override val name: String?,
//) : StubBase<MvModuleDef>(parent, elementType),
//    MvNamedElementStub {
//
//    object Type : MvStubElementType<MvModuleDefStub, MvModuleDef>("MODULE_DEF") {
//
//        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
//            MvModuleDefStub(
//                parentStub,
//                this,
//                dataStream.readNameString()!!,
//                dataStream.readNameString(),
//            )
//
//        override fun serialize(stub: MvModuleDefStub, dataStream: StubOutputStream) =
//            with(dataStream) {
//                writeName(stub.address)
//                writeName(stub.name)
//            }
//
//        override fun createPsi(stub: MvModuleDefStub): MvModuleDef =
//            MvModuleDefImpl(stub, this)
//
//        override fun createStub(psi: MvModuleDef, parentStub: StubElement<*>?) =
//            MvModuleDefStub(parentStub, this, psi.containingAddress.text, psi.name)
//
//        override fun indexStub(stub: MvModuleDefStub, sink: IndexSink) = MvModulesIndex.index(stub, sink)
//    }
//}
