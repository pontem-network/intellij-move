package org.move.lang.core.completion.providers

//object ImportsCompletionProvider: MvCompletionProvider() {
//    override val elementPattern: ElementPattern<PsiElement>
//        get() = PlatformPatterns
//            .psiElement().withParent<MvUseItem>()
//
//    override fun addCompletions(
//        parameters: CompletionParameters,
//        context: ProcessingContext,
//        result: CompletionResultSet
//    ) {
//        val itemImport = parameters.position.parent as MvUseItem
//        if (parameters.position !== itemImport.referenceNameElement) return
//
//        val moduleRef = itemImport.itemUseSpeck.fqModuleRef
//        val referredModule = moduleRef.reference?.resolve() as? MvModule
//            ?: return
//
//        val p = itemImport.parent
//        if (p is MvUseItemGroup && "Self" !in p.names) {
//            result.addElement(referredModule.createSelfLookup())
//        }
//
//        val vs = when {
//            moduleRef.isSelfModuleRef -> setOf(Visibility.Internal)
//            else -> Visibility.visibilityScopesForElement(itemImport)
//        }
//        val ns = setOf(Namespace.NAME, Namespace.TYPE, Namespace.FUNCTION)
//        val contextScopeInfo =
//            ContextScopeInfo(
//                letStmtScope = itemImport.letStmtScope,
//                refItemScopes = itemImport.refItemScopes,
//            )
//
//        val completionContext = CompletionContext(itemImport, contextScopeInfo)
//        processModuleItems(referredModule, ns, vs, contextScopeInfo) {
//            result.addElement(
//                it.element.createLookupElement(
//                    completionContext,
//                    insertHandler = BasicInsertHandler(),
//                    structAsType = true
//                )
//            )
//            false
//        }
//    }
//}
