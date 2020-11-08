package org.move.lang.core.stubs.impl

//class MoveModuleDefStub(
//    parent: StubElement<*>?,
//    elementType: IStubElementType<*, *>,
//    val address: String,
//    override val name: String?,
//) : StubBase<MoveModuleDef>(parent, elementType),
//    MoveNamedElementStub {
//
//    object Type : MoveStubElementType<MoveModuleDefStub, MoveModuleDef>("MODULE_DEF") {
//
//        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
//            MoveModuleDefStub(
//                parentStub,
//                this,
//                dataStream.readNameString()!!,
//                dataStream.readNameString(),
//            )
//
//        override fun serialize(stub: MoveModuleDefStub, dataStream: StubOutputStream) =
//            with(dataStream) {
//                writeName(stub.address)
//                writeName(stub.name)
//            }
//
//        override fun createPsi(stub: MoveModuleDefStub): MoveModuleDef =
//            MoveModuleDefImpl(stub, this)
//
//        override fun createStub(psi: MoveModuleDef, parentStub: StubElement<*>?) =
//            MoveModuleDefStub(parentStub, this, psi.containingAddress.text, psi.name)
//
//        override fun indexStub(stub: MoveModuleDefStub, sink: IndexSink) = MoveModulesIndex.index(stub, sink)
//    }
//}