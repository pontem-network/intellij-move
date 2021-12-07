package org.move.lang.core.stubs

interface MvNamedElementStub {
    val name: String?
}

//abstract class MvNamedElementStub<PsiT : MvNamedElement> : NamedStubBase<PsiT> {
//    constructor(parent: StubElement<*>?, elementType: IStubElementType<*, *>, name: StringRef)
//            : super(parent, elementType, name)
//
//    constructor(parent: StubElement<*>?, elementType: IStubElementType<*, *>, name: String)
//            : super(parent, elementType, name)
//
//}
