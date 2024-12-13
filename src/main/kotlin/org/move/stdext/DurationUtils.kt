package org.move.stdext

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

fun now(): Duration = System.nanoTime().nanoseconds
