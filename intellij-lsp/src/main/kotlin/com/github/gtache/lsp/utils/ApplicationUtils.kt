package com.github.gtache.lsp.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import java.util.concurrent.Callable
import java.util.concurrent.Future

/**
 * Various methods to write thread related instructions more concisely
 */
object ApplicationUtils {

    /**
     * Runs the given [runnable] on the EDT thread
     */
    @JvmStatic
    fun invokeLater(runnable: Runnable): Unit {
        return ApplicationManager.getApplication().invokeLater(runnable)
    }

    /**
     * Runs the given [runnable] on a background thread
     */
    @JvmStatic
    fun pool(runnable: Runnable): Unit {
        ApplicationManager.getApplication().executeOnPooledThread(runnable)
    }

    /**
     * Runs the given [callable] on a background thread and returns its value
     */
    @JvmStatic
    fun <T> callablePool(callable: Callable<T>): Future<T> {
        return ApplicationManager.getApplication().executeOnPooledThread(callable)
    }

    /**
     * Runs the given [computable] on the EDT thread in a read action and returns its value
     */
    @JvmStatic
    fun <T> computableReadAction(computable: Computable<T>): T {
        return ApplicationManager.getApplication().runReadAction(computable)
    }

    /**
     * Runs the given [runnable] on the EDT thread in a write action
     */
    @JvmStatic
    fun writeAction(runnable: Runnable): Unit {
        return ApplicationManager.getApplication().runWriteAction(runnable)
    }

    /**
     * Runs the given [computable] on the EDT thread in a write action and returns its value
     */
    @JvmStatic
    fun <T> computableWriteAction(computable: Computable<T>): T {
        return ApplicationManager.getApplication().runWriteAction(computable)
    }
}