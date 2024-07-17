package org.move.ide.utils.imports

import org.move.lang.MoveFile
import org.move.lang.core.psi.MvQualNamedElement
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.ItemQualName
import org.move.openapiext.common.checkUnitTestMode

data class ImportCandidate(val element: MvQualNamedElement, val qualName: ItemQualName)

//fun processFileItemsForUnitTests(
//    file: MoveFile,
//    namespaces: Set<Namespace>,
//    visibilities: Set<Visibility>,
//    contextScopeInfo: ContextScopeInfo,
//    processor: RsResolveProcessor,
//): Boolean {
//    checkUnitTestMode()
//    val contextProcessor = contextScopeInfo.wrapWithContextFilter(processor)
//    for (module in file.modules()) {
//        if (
//            Namespace.MODULE in namespaces && contextProcessor.process(module)
//        ) {
//            return true
//        }
//        val matchProcessor = MatchingProcessor {
//            contextProcessor.process(it.name, it.element)
//        }
//        if (processModuleInnerItems(module, namespaces, visibilities, contextScopeInfo, matchProcessor)) return true
//    }
//    return false
//}
