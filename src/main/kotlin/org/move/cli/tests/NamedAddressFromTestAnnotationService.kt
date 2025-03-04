package org.move.cli.tests

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.move.cli.MoveProject
import org.move.lang.core.types.NumericAddress

val Project.testAddressesService: NamedAddressFromTestAnnotationService get() = this.service()

interface NamedAddressFromTestAnnotationService {
    fun getNamedAddress(moveProject: MoveProject, name: String): String?
    fun getNamedAddressesForValue(moveProject: MoveProject, numericAddress: NumericAddress): List<String>
}

class NamedAddressServiceImpl: NamedAddressFromTestAnnotationService {
    override fun getNamedAddress(moveProject: MoveProject, name: String): String? = null
    override fun getNamedAddressesForValue(moveProject: MoveProject, numericAddress: NumericAddress): List<String> = emptyList()
}


class NamedAddressServiceTestImpl: NamedAddressFromTestAnnotationService {
    val namedAddresses: MutableMap<String, String> = mutableMapOf()

    override fun getNamedAddress(moveProject: MoveProject, name: String): String? = namedAddresses[name]

    override fun getNamedAddressesForValue(moveProject: MoveProject, numericAddress: NumericAddress): List<String> {
        return this.namedAddresses
            .filterValues { NumericAddress(it) == numericAddress }
            .map { it.key }
    }
}