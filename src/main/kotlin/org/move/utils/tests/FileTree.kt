/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests

fun replaceCaretMarker(text: String): String = text.replace("/*caret*/", "<caret>")

fun hasCaretMarker(text: String): Boolean = text.contains("/*caret*/") || text.contains("<caret>")
