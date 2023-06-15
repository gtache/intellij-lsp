package com.github.gtache.lsp

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Prepends an [obj] to a list and returns the list
 */
fun <T> MutableList<T>?.prepend(obj: T): MutableList<T>? {
    this?.add(0, obj)
    return this
}

/**
 * Filters a map by removing null values
 */
fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> {
    return mapNotNull { (key, nullableValue) ->
        nullableValue?.let { key to it }
    }.toMap()
}

/**
 * Returns the first character of a string
 */
val String.head: Char
    get() = this.first()

/**
 * Returns all but the first element of a string
 */
val String.tail: String
    get() = this.drop(1)

/**
 * Returns the first element of an iterable
 */
val <T> Iterable<T>.head: T
    get() = this.first()

/**
 * Returns the first element of an iterable, or null if not available
 */
val <T> Iterable<T>.headOrNull: T?
    get() = this.firstOrNull()

/**
 * Returns all but the first element of an iterable
 */
val <T> Iterable<T>.tail: List<T>
    get() = this.drop(1)

/**
 * Returns the first element of an array
 */
val <T> Array<T>.head: T
    get() = this.first()

/**
 * Returns all but the first element of an array
 */
val <T> Array<T>.tail: List<T>
    get() = this.drop(1)

/**
 * Returns the first key->value pair of a map
 */
val <T, U> Map<T, U>.head: Map.Entry<T, U>
    get() = this.entries.iterator().next()

/**
 * Catches multiple exception [classes] (similar to Java '|' operator), executes the given [block] and returns its result.
 */
fun <R> Throwable.multicatch(vararg classes: KClass<*>, block: () -> R): R {
    if (classes.any { this::class.isSubclassOf(it) }) {
        return block()
    } else throw this
}

/**
 * Reverses a map from key -> value to value -> key
 */
fun <K, V> Map<K, V>.reversed(): Map<V, K> = HashMap<V, K>().also { newMap ->
    entries.forEach { newMap[it.value] = it.key }
}

/**
 * Runs a string command in [workingDir] and returns the output of the process
 */
fun String.runCommand(workingDir: File): String? {
    return try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        proc.waitFor(1, TimeUnit.MINUTES)
        proc.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}