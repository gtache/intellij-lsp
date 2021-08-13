package com.github.gtache.lsp.requests

/**
 * Enumeration for the timeouts
 */
enum class Timeouts(val defaultTimeout: Long) {
    CODEACTION(2000L),
    CODELENS(2000L),
    COMPLETION(1000L),
    DEFINITION(2000L),
    DOC_HIGHLIGHT(1000L),
    EXECUTE_COMMAND(2000L),
    FORMATTING(2000L),
    HOVER(2000L),
    INIT(10000L),
    PREPARE_RENAME(2000L),
    REFERENCES(2000L),
    SIGNATURE(1000L),
    SHUTDOWN(5000L),
    SYMBOLS(2000L),
    WILLSAVE(2000L);
}