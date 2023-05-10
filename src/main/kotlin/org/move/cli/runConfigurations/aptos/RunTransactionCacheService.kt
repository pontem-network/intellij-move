package org.move.cli.runConfigurations.aptos

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "RunTransactionCacheService",
)
class RunTransactionCacheService(
    val project: Project
) :
    SimplePersistentStateComponent<RunTransactionCacheService.MyState>(MyState(mutableMapOf())) {

    data class MyState(
        val typeParamsCache: MutableMap<String, MutableList<String>>
    ) : BaseState()

    fun cacheTypeParameter(param: String, value: String) {
        val paramCache = this.state.typeParamsCache[param]
        if (paramCache != null) {
            if (value in paramCache) return
            paramCache.add(value)
        } else {
            this.state.typeParamsCache[param] = mutableListOf(value)
        }
        this.state.intIncrementModificationCount()
    }

    fun getTypeParameterCache(param: String): List<String> {
        return this.state.typeParamsCache[param] ?: emptyList()
    }
}

val Project.cacheService: RunTransactionCacheService get() = service()
