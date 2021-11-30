package org.move.toml

import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlTableHeader

val TomlKeySegment.isDependencyKey: Boolean
    get() {
        val name = name
        return name == "dependencies" || name == "dev-dependencies"
    }

val TomlTableHeader.isDependencyListHeader: Boolean
    get() = key?.segments?.lastOrNull()?.isDependencyKey == true
