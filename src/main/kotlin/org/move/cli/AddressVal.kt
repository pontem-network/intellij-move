package org.move.cli

import org.move.openapiext.singleSegmentOrNull
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue

typealias RawAddressVal = Pair<String, TomlKeyValue>

data class AddressVal(
    val value: String,
    val keyValue: TomlKeyValue?,
    val placeholderKeyValue: TomlKeyValue?,
    val packageName: String
) {
    val tomlKeySegment: TomlKeySegment?
        get() {
            return this.placeholderKeyValue?.singleSegmentOrNull()
                ?: this.keyValue?.singleSegmentOrNull()
        }
}

data class PlaceholderVal(
    val keyValue: TomlKeyValue,
    val packageName: String,
)

typealias RawAddressMap = MutableMap<String, RawAddressVal>

typealias AddressMap = MutableMap<String, AddressVal>
typealias PlaceholderMap = MutableMap<String, PlaceholderVal>

typealias DependenciesMap = MutableMap<String, Dependency>
typealias DepsSubstMap = MutableMap<String, Pair<Dependency, RawAddressMap>>

fun DepsSubstMap.asDependenciesMap(): DependenciesMap {
    return this.mapValues { it.value.first }.toMutableMap()
}

fun mutableRawAddressMap(): RawAddressMap = mutableMapOf()
fun mutableAddressMap(): AddressMap = mutableMapOf()
fun dependenciesMap(): DependenciesMap = mutableMapOf()
fun placeholderMap(): PlaceholderMap = mutableMapOf()

fun AddressMap.copyMap(): AddressMap = this.toMutableMap()
