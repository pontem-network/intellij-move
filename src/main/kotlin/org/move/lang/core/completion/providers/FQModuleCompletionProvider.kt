package org.move.lang.core.completion.providers

//object FQModuleCompletionProvider: MvCompletionProvider() {
//    override val elementPattern: ElementPattern<out PsiElement>
//        get() =
//            PlatformPatterns.psiElement()
//                .withParent<MvFQModuleRef>()
//
//    override fun addCompletions(
//        parameters: CompletionParameters,
//        context: ProcessingContext,
//        result: CompletionResultSet,
//    ) {
//        val directParent = parameters.position.parent
//        val fqModuleRef =
//            directParent as? MvFQModuleRef
//                ?: directParent.parent as MvFQModuleRef
//        if (parameters.position !== fqModuleRef.referenceNameElement) return
//
//        val contextScopeInfo = ContextScopeInfo(
//            letStmtScope = fqModuleRef.letStmtScope,
//            refItemScopes = fqModuleRef.refItemScopes,
//        )
//        val completionContext = CompletionContext(fqModuleRef, contextScopeInfo)
//
//        val moveProj = fqModuleRef.moveProject
//        val positionAddress = fqModuleRef.addressRef.address(moveProj)
//
//        processFQModuleRef(fqModuleRef) {
//            val module = it.element
//            val moduleAddress = module.address(moveProj)
//            if (Address.eq(positionAddress, moduleAddress)) {
//                val lookup = module.createLookupElement(completionContext)
//                result.addElement(lookup)
//            }
//            false
//        }
//    }
//}
