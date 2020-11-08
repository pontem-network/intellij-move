package org.move.lang.core.stubs

interface MoveNamedElementStub {
    val name: String?
}

//abstract class MoveNamedElementStub<PsiT : MoveNamedElement> : NamedStubBase<PsiT> {
//    constructor(parent: StubElement<*>?, elementType: IStubElementType<*, *>, name: StringRef)
//            : super(parent, elementType, name)
//
//    constructor(parent: StubElement<*>?, elementType: IStubElementType<*, *>, name: String)
//            : super(parent, elementType, name)
//
//}