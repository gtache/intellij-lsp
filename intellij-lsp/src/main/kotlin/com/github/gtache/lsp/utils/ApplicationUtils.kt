package com.github.gtache.lsp.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import java.util.concurrent.Callable
import java.util.concurrent.Future

/**
 * Various methods to write thread related instructions more concisely
 */
object ApplicationUtils {

    @JvmStatic
    fun invokeLater(runnable: Runnable): Unit {
        return ApplicationManager.getApplication().invokeLater(runnable)
    }

    @JvmStatic
    fun pool(runnable: Runnable): Unit {
        ApplicationManager.getApplication().executeOnPooledThread(runnable)
    }

    @JvmStatic
    fun <T> callablePool(callable: Callable<T>): Future<T> {
        return ApplicationManager.getApplication().executeOnPooledThread(callable)
    }

    @JvmStatic
    fun <T> computableReadAction(computable: Computable<T>): T {
        return ApplicationManager.getApplication().runReadAction(computable)
    }

    @JvmStatic
    fun writeAction(runnable: Runnable): Unit {
        return ApplicationManager.getApplication().runWriteAction(runnable)
    }

    @JvmStatic
    fun <T> computableWriteAction(computable: Computable<T>): T {
        return ApplicationManager.getApplication().runWriteAction(computable)
    }
}