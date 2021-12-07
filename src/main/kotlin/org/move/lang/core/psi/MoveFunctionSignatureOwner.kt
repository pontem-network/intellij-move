package org.move.lang.core.psi

interface MvFunctionSignatureOwner : MvTypeParametersOwner {
    val functionParameterList: MvFunctionParameterList?
    val returnType: MvReturnType?
}

val MvFunctionSignatureOwner.parameters: List<MvFunctionParameter>
    get() =
        this.functionParameterList?.functionParameterList.orEmpty()
