package com.github.gtache.lsp.requests

import com.github.gtache.lsp.requests.Timeouts.*

/**
 * An object containing the timeout values for the various requests
 */
object Timeout {

    var timeouts: Map<Timeouts, Long> = Timeouts.values().associateWith { t -> t.defaultTimeout }

    fun CODEACTION_TIMEOUT(): Long = timeouts[CODEACTION]!!

    fun CODELENS_TIMEOUT(): Long = timeouts[CODELENS]!!

    fun COMPLETION_TIMEOUT(): Long = timeouts[COMPLETION]!!

    fun DEFINITION_TIMEOUT(): Long = timeouts[DEFINITION]!!

    fun DOC_HIGHLIGHT_TIMEOUT(): Long = timeouts[DOC_HIGHLIGHT]!!

    fun EXECUTE_COMMAND_TIMEOUT(): Long = timeouts[EXECUTE_COMMAND]!!

    fun FORMATTING_TIMEOUT(): Long = timeouts[FORMATTING]!!

    fun HOVER_TIMEOUT(): Long = timeouts[HOVER]!!

    fun INIT_TIMEOUT(): Long = timeouts[INIT]!!

    fun PREPARE_RENAME_TIMEOUT(): Long = timeouts[PREPARE_RENAME]!!

    fun REFERENCES_TIMEOUT(): Long = timeouts[REFERENCES]!!

    fun SIGNATURE_TIMEOUT(): Long = timeouts[SIGNATURE]!!

    fun SHUTDOWN_TIMEOUT(): Long = timeouts[SHUTDOWN]!!

    fun SYMBOLS_TIMEOUT(): Long = timeouts[SYMBOLS]!!

    fun WILLSAVE_TIMEOUT(): Long = timeouts[WILLSAVE]!!
}