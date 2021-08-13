package com.github.gtache.lsp

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


fun <T> MutableList<T>?.prepend(obj: T): Unit {
    this?.add(0, obj)
}

val String.head
    get() = this.first()
val String.tail
    get() = this.drop(1)
val <T> Iterable<T>.head
    get() = this.first()
val <T> Iterable<T>.headOrNull
    get() = this.firstOrNull()
val <T> Iterable<T>.tail
    get() = this.drop(1)

val <T> Array<T>.head
    get() = this.first()
val <T> Array<T>.tail
    get() = this.drop(1)

val <T, U> Map<T, U>.head
    get() = this.entries.iterator().next()

fun <R> Throwable.multicatch(vararg classes: KClass<*>, block: () -> R): R {
    if (classes.any { this::class.isSubclassOf(it) }) {
        return block()
    } else throw this
}

fun <K, V> Map<K, V>.reversed() = HashMap<V, K>().also { newMap ->
    entries.forEach { newMap[it.value] = it.key }
}

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
    } catch(e: IOException) {
        e.printStackTrace()
        null
    }
}