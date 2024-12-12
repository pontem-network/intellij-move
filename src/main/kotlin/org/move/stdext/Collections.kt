/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.stdext

import com.intellij.util.SmartList
import java.util.*
import kotlin.reflect.full.memberProperties

@Suppress("UNCHECKED_CAST")
inline fun <T> buildList(builder: (CollectionBuilder<T>).() -> Unit): List<T> =
    buildCollection(mutableListOf(), builder) as List<T>

@Suppress("UNCHECKED_CAST")
inline fun <T> buildSet(builder: (CollectionBuilder<T>).() -> Unit): Set<T> =
    buildCollection(mutableSetOf(), builder) as Set<T>

inline fun <T> buildCollection(
    result: MutableCollection<T>,
    builder: (CollectionBuilder<T>).() -> Unit,
): MutableCollection<T> {
    object : CollectionBuilder<T> {
        override fun add(item: T) {
            result.add(item)
        }

        override fun addAll(items: Collection<T>) {
            result.addAll(items)
        }
    }.builder()
    return result
}

interface CollectionBuilder<in T> {
    fun add(item: T)
    fun addAll(items: Collection<T>)
}

inline fun <K, V> buildMap(builder: (MapBuilder<K, V>).() -> Unit): Map<K, V> {
    val result = HashMap<K, V>()
    object : MapBuilder<K, V> {
        override fun put(key: K, value: V) {
            result[key] = value
        }

        override fun putAll(map: Map<K, V>) {
            result.putAll(map)
        }
    }.builder()

    return result.replaceTrivialMap()
}

interface MapBuilder<K, in V> {
    fun put(key: K, value: V)
    fun putAll(map: Map<K, V>)
}

fun <K, V> Map<K, V>.replaceTrivialMap(): Map<K, V> = when (size) {
    0 -> emptyMap()
    1 -> {
        val entry = entries.single()
        Collections.singletonMap(entry.key, entry.value)
    }
    else -> this
}

fun <T> SmartList<T>.optimizeList(): List<T> = when (size) {
    0 -> emptyList()
    1 -> Collections.singletonList(single())
    else -> {
        trimToSize()
        this
    }
}

private const val INT_MAX_POWER_OF_TWO: Int = Int.MAX_VALUE / 2 + 1

/* Copied from Kotlin's internal Maps.kt */
fun mapCapacity(expectedSize: Int): Int {
    if (expectedSize < 3) {
        return expectedSize + 1
    }
    if (expectedSize < INT_MAX_POWER_OF_TWO) {
        return expectedSize + expectedSize / 3
    }
    return Int.MAX_VALUE // any large value
}

/* Copied from Kotlin's internal Iterables.kt */
fun <T> Iterable<T>.collectionSizeOrDefault(default: Int): Int =
    if (this is Collection<*>) size else default

fun makeBitMask(bitToSet: Int): Int = 1 shl bitToSet

fun <K, V1, V2> zipValues(map1: Map<K, V1>, map2: Map<K, V2>): List<Pair<V1, V2>> =
    map1.mapNotNull { (k, v1) -> map2[k]?.let { v2 -> Pair(v1, v2) } }

inline fun <T> List<T>.singleOrFilter(predicate: (T) -> Boolean): List<T> = when {
    size < 2 -> this
    else -> filter(predicate)
}

inline fun <T> List<T>.singleOrLet(function: (List<T>) -> List<T>): List<T> = when {
    size < 2 -> this
    else -> function(this)
}

inline fun <T> List<T>.notEmptyOrLet(function: (List<T>) -> List<T>): List<T> = when {
    isNotEmpty() -> this
    else -> function(this)
}

fun <T> List<T>.chain(other: List<T>): Sequence<T> =
    when {
        other.isEmpty() -> this.asSequence()
        this.isEmpty() -> other.asSequence()
        else -> this.asSequence() + other.asSequence()
    }

inline fun <T, R> Iterable<T>.mapToMutableList(transform: (T) -> R): MutableList<R> =
    mapTo(ArrayList(collectionSizeOrDefault(10)), transform)

inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> =
    mapTo(HashSet(mapCapacity(collectionSizeOrDefault(10))), transform)

inline fun <T, R : Any> Iterable<T>.mapNotNullToSet(transform: (T) -> R?): Set<R> =
    mapNotNullTo(HashSet(mapCapacity(collectionSizeOrDefault(10))), transform)

fun <T> Set<T>.intersects(other: Iterable<T>): Boolean = other.any { this.contains(it) }

fun <T> Set<T>.containsAny(vararg items: T): Boolean = items.any { this.contains(it) }

inline fun <T> Iterable<T>.joinToWithBuffer(
    buffer: StringBuilder,
    sep: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    action: T.(StringBuilder) -> Unit,
) {
    buffer.append(prefix)
    var needInsertSeparator = false
    for (element in this) {
        if (needInsertSeparator) {
            buffer.append(sep)
        }
        element.action(buffer)
        needInsertSeparator = true
    }
    buffer.append(postfix)
}

fun <T : Any> Iterator<T>.nextOrNull(): T? =
    if (hasNext()) next() else null

inline fun <reified T : Enum<T>> enumSetOf(): EnumSet<T> = EnumSet.noneOf(T::class.java)

typealias LookbackValue<T> = Pair<T, T?>

fun <T> Sequence<T>.withPrevious(): Sequence<LookbackValue<T>> = LookbackSequence(this)

private class LookbackSequence<T>(private val sequence: Sequence<T>) : Sequence<LookbackValue<T>> {

    override fun iterator(): Iterator<LookbackValue<T>> = LookbackIterator(sequence.iterator())
}

private class LookbackIterator<T>(private val iterator: Iterator<T>) : Iterator<LookbackValue<T>> {

    private var previous: T? = null

    override fun hasNext() = iterator.hasNext()

    override fun next(): LookbackValue<T> {
        val next = iterator.next()
        val result = LookbackValue(next, previous)
        previous = next
        return result
    }
}

fun <K, V> MutableMap<K, MutableList<V>>.putGrouped(key: K, value: V) {
    getOrPut(key) { mutableListOf() }.add(value)
}

fun <T> T?.wrapWithList(): List<T> = this?.let { listOf(it) }.orEmpty()
fun <T> T?.wrapWithMutableList(): MutableList<T> = this?.let { listOf(it) }.orEmpty().toMutableList()

fun <T> List<T>.withAdded(element: T): List<T> = listOf(this, listOf(element)).flatten()
fun <T> List<T>.withExtended(list: List<T>): List<T> = listOf(this, list).flatten()

typealias WithNextValue<T> = Pair<T, T?>

fun <T : Any> Sequence<T>.withNext(): Sequence<WithNextValue<T>> = WithNextSequence(this)

private class WithNextSequence<T : Any>(private val sequence: Sequence<T>) : Sequence<WithNextValue<T>> {

    override fun iterator(): Iterator<WithNextValue<T>> = WithNextIterator(sequence.iterator())
}

private class WithNextIterator<T : Any>(private val iterator: Iterator<T>) : Iterator<WithNextValue<T>> {

    private var next: T? = null

    override fun hasNext() = next != null || iterator.hasNext()

    override fun next(): WithNextValue<T> {
        if (next == null) { // The first invocation (or illegal after-the-last invocation)
            next = iterator.next()
        }
        val next = next ?: throw NoSuchElementException()
        val nextNext = iterator.nextOrNull()
        this.next = nextNext
        return WithNextValue(next, nextNext)
    }
}

inline fun <reified T : Any> T.asMap() : Map<String, Any?> {
    val props = T::class.memberProperties.associateBy { it.name }
    return props.keys.associateWith { props[it]?.get(this) }
}

inline fun <T> T.applyIf(condition: Boolean, body: T.() -> Unit) {
    if (!condition) {
        this.body()
    }
}

inline fun <T> T.chainIf(condition: Boolean, body: T.() -> T): T {
    return if (!condition) {
        this
    } else {
        this.body()
    }
}

//inline fun <T, U> T.mapIf(condition: Boolean, fn: (T) -> T): T {
//    if (!condition) {
//        return this
//    } else {
//        return this.body()
//    }
//}
