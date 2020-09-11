package org.move.lang.core.psi

interface MoveFunctionSignatureOwner : MoveElement {
    val functionParameterList: MoveFunctionParameterList?
    val returnType: MoveReturnType?
}

val MoveFunctionSignatureOwner.parameters: List<MoveFunctionParameter>
    get() =
        this.functionParameterList?.functionParameterList.orEmpty()
