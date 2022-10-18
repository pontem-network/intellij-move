/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.lang.core.types.infer

interface DAGNodeOrValue
interface DAGNode : DAGNodeOrValue {
    var next: DAGNodeOrValue
}

data class DAGValue<out V>(val value: V?) : DAGNodeOrValue

/**
 * [UnificationTable] is map from [K] to [V] with additional ability
 * to redirect certain K's to a single V en-masse with the help of
 * disjoint set union.
 *
 * We implement Tarjan's union-find
 * algorithm: when two keys are unified, one of them is converted
 * into a "redirect" pointing at the other. These redirects form a
 * DAG: the roots of the DAG (nodes that are not redirected) are each
 * associated with a value of type `V` and a rank. The rank is used
 * to keep the DAG relatively balanced, which helps keep the running
 * time of the algorithm under control. For more information, see
 * <http://en.wikipedia.org/wiki/Disjoint-set_data_structure>.
 *
 * TODO this class should provide snapshot-rollback feature
 */
@Suppress("UNCHECKED_CAST")
class UnificationTable<K : DAGNode, V> {
    @Suppress("UNCHECKED_CAST")
    private data class Root<out K : DAGNode, out V>(val key: K) {
        val value: V?
            get() = (key.next as DAGValue<V>).value
//        private val varValue: VarValue<V> = key.redirectsTo as VarValue<V>
//        val value: V? get() = varValue.value
    }

    private fun getRoot(key: DAGNode): Root<K, V> {
        val node = key.next
        if (node !is DAGNode) return Root(key as K)
        val root = getRoot(node)
        key.next = root.key
        return root
    }

    fun findRoot(key: K): K = getRoot(key).key

    fun findValue(key: K): V? = getRoot(key).value

    fun unifyVarVar(key1: K, key2: K): K {
        val node1Root = getRoot(key1)
        val node2Root = getRoot(key2)

        if (node1Root.key == node2Root.key) return node1Root.key // already unified

        val val1 = node1Root.value
        val val2 = node2Root.value

        val newVal = if (val1 != null && val2 != null) {
            if (val1 != val2) error("unification error") // must be solved on the upper level
            val1
        } else {
            val1 ?: val2
        }

//        val oldRootKey = node1Root.key
        val newRootKey = node2Root.key
        node1Root.key.next = newRootKey
        newRootKey.next = DAGValue<V>(newVal)
        return newRootKey
    }

    fun unifyVarValue(key: K, value: V) {
        val root = getRoot(key)
        if (root.value != null && root.value != value) {
            error("unification error, (root.value = ${root.value}) != (value = $value) ") // must be solved on the upper level
        }

        root.key.next = DAGValue<V>(value)
    }
}
