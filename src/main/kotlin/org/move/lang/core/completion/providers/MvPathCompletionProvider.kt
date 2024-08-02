package org.move.lang.core.completion.providers

//fun interface CompletionFilter {
//    fun removeEntry(entry: ScopeEntry, ctx: PathResolutionContext): Boolean
//}

//abstract class MvPathCompletionProvider: MvCompletionProvider() {
//
//    abstract val namespaces: Set<Namespace>
//
//    open val completionFilters: List<CompletionFilter> = emptyList()
//
//    open fun pathScopeInfo(pathElement: MvPath): ContextScopeInfo =
//        ContextScopeInfo(
//            letStmtScope = pathElement.letStmtScope,
//            refItemScopes = pathElement.refItemScopes,
//        )
//
//    final override fun addCompletions(
//        parameters: CompletionParameters,
//        context: ProcessingContext,
//        result: CompletionResultSet
//    ) {
//        val maybePath = parameters.position.parent
//        val pathElement = maybePath as? MvPath ?: maybePath.parent as MvPath
//
//        if (parameters.position !== pathElement.referenceNameElement) return
//
//        val qualifier = pathElement.qualifier
//
//        val contextScopeInfo = pathScopeInfo(pathElement)
//        val msl = pathElement.isMslScope
//        val expectedTy = getExpectedTypeForEnclosingPathOrDotExpr(pathElement, msl)
//        val structAsType = Namespace.TYPE in this.namespaces
//
//        val completionContext = CompletionContext(
//            pathElement,
//            contextScopeInfo,
//            expectedTy,
//        )
//
//        var completionCollector = createProcessor { e ->
//            val element = e.element as? MvNamedElement ?: return@createProcessor
//            val lookup =
//                element.createLookupElement(
//                    completionContext,
//                    structAsType = structAsType,
//                    priority = element.completionPriority
//                )
//            result.addElement(lookup)
//        }
//
//        if (qualifier != null) {
//            val resolvedQualifier = qualifier.reference?.resolveFollowingAliases()
//            when (resolvedQualifier) {
//                is MvModule -> {
//                    val moduleBlock = resolvedQualifier.moduleBlock
//                    if (moduleBlock != null) {
//                        processItemDeclarations(moduleBlock, this.namespaces, completionCollector)
//                    }
//                }
//            }
//            return
//        }
//
////        if (moduleRef != null) {
////            val module = moduleRef.reference?.resolveWithAliases() as? MvModule
////                ?: return
////            val vs = when {
////                moduleRef.isSelfModuleRef -> setOf(Visibility.Internal)
////                else -> Visibility.visibilityScopesForElement(pathElement)
////            }
////            processModuleItems(module, namespaces, vs, contextScopeInfo) {
////                val lookup =
////                    it.element.createLookupElement(ctx, structAsType = structAsType)
////                result.addElement(lookup)
////                false
////            }
////            return
////        }
//
//        val processedNames = mutableSetOf<String>()
//        completionCollector = completionCollector.wrapWithFilter { e ->
//            if (processedNames.contains(e.name)) {
//                return@wrapWithFilter false
//            }
//            processedNames.add(e.name)
//            true
//        }
//
//        val resolutionCtx = PathResolutionContext(pathElement, contextScopeInfo)
//
//        // custom filters
//        completionCollector = completionCollector.wrapWithFilter {
//            for (completionFilter in this.completionFilters) {
//                if (!completionFilter.removeEntry(it, resolutionCtx)) return@wrapWithFilter false
//            }
//            true
//        }
//
//        val ctx = PathResolutionContext(pathElement, contextScopeInfo)
//        processNestedScopesUpwards(pathElement, this.namespaces, ctx, completionCollector)
//
////        processItems(pathElement, namespaces, contextScopeInfo) { (name, element) ->
////            if (processedNames.contains(name)) {
////                return@processItems false
////            }
////            processedNames.add(name)
////            result.addElement(
////                element.createLookupElement(
////                    completionContext,
////                    structAsType = structAsType,
////                    priority = element.completionPriority
////                )
////            )
////            false
////        }
//
//        // disable auto-import in module specs for now
//        if (pathElement.containingModuleSpec != null) return
//
//        val originalPathElement = parameters.originalPosition?.parent as? MvPath ?: return
//        val importContext =
//            ImportContext.from(
//                originalPathElement,
//                this.namespaces,
//                setOf(Visibility.Public),
//                contextScopeInfo
//            )
//        val candidates = getCompletionCandidates(
//            parameters,
//            result.prefixMatcher,
//            processedNames,
//            importContext,
//        )
//        candidates.forEach { candidate ->
//            val entry = SimpleScopeEntry(candidate.qualName.itemName, candidate.element, namespaces)
//            for (completionFilter in completionFilters) {
//                if (!completionFilter.removeEntry(entry, resolutionCtx)) return@forEach
//            }
//            val lookupElement = candidate.element.createLookupElement(
//                completionContext,
//                structAsType = structAsType,
//                priority = UNIMPORTED_ITEM_PRIORITY,
//                insertHandler = ImportInsertHandler(parameters, candidate)
//            )
//            result.addElement(lookupElement)
//        }
//    }
//}

//object NamesCompletionProvider: MvPathCompletionProvider() {
//    override val elementPattern: ElementPattern<PsiElement>
//        get() =
//            MvPsiPatterns.path()
//                .andNot(MvPsiPatterns.pathType())
//                .andNot(MvPsiPatterns.schemaLit())
//
//    override val namespaces: Set<Namespace> get() = EnumSet.of(Namespace.NAME)
//}

//object FunctionsCompletionProvider: MvPathCompletionProvider() {
//    override val elementPattern: ElementPattern<PsiElement>
//        get() =
//            MvPsiPatterns.path()
//                .andNot(MvPsiPatterns.pathType())
//                .andNot(MvPsiPatterns.schemaLit())
//
//    override val namespaces: Set<Namespace> get() = EnumSet.of(Namespace.FUNCTION)
//}

//object TypesCompletionProvider: MvPathCompletionProvider() {
//    override val elementPattern: ElementPattern<out PsiElement>
//        get() = MvPsiPatterns.pathType()
//
//    override val namespaces: Set<Namespace> get() = EnumSet.of(Namespace.TYPE)
//}

//object SchemasCompletionProvider: MvPathCompletionProvider() {
//    override val elementPattern: ElementPattern<PsiElement>
//        get() =
//            StandardPatterns.or(
//                MvPsiPatterns.schemaLit(), MvPsiPatterns.pathInsideIncludeStmt()
//            )
//
//    override val namespaces: Set<Namespace> get() = EnumSet.of(Namespace.SCHEMA)
//
//    override fun pathScopeInfo(pathElement: MvPath): ContextScopeInfo {
//        return ContextScopeInfo(
//            letStmtScope = LetStmtScope.EXPR_STMT,
//            refItemScopes = pathElement.refItemScopes,
//        )
//    }
//}

//object ModulesCompletionProvider: MvPathCompletionProvider() {
//    override val elementPattern: ElementPattern<PsiElement>
//        get() =
//            MvPsiPatterns.path()
//                .andNot(MvPsiPatterns.pathType())
//                .andNot(MvPsiPatterns.schemaLit())
//
//    override val namespaces: Set<Namespace> get() = EnumSet.of(Namespace.MODULE)
//
//    override val completionFilters: List<CompletionFilter>
//        get() = listOf(
//            // filter out the current module
//            CompletionFilter { e, ctx ->
//                if (e.element is MvModule)
//                    return@CompletionFilter ctx.containingModule?.let { e.element.equalsTo(it) } ?: true
//                true
//            })
//}
