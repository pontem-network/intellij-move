package org.move.utils.tests

import org.move.cli.MoveProject
import org.move.lang.core.types.Address
import org.move.lang.core.types.Address.Named

interface NamedAddressService {
    fun getNamedAddress(moveProject: MoveProject, name: String): Named?
}

class NamedAddressServiceImpl: NamedAddressService {
    override fun getNamedAddress(moveProject: MoveProject, name: String): Named? = null
}

class NamedAddressServiceTestImpl: NamedAddressService {
    val namedAddresses: MutableMap<String, String> = mutableMapOf()

    override fun getNamedAddress(moveProject: MoveProject, name: String): Named? {
        val value = namedAddresses[name] ?: return null
        return Named(name, value, moveProject)
    }
}