package com.github.gtache.lsp.client;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum Methods {
    DID_CHANGE_CONFIGURATION("workspace/didChangeConfiguration"),
    DID_CHANGE_WATCHED_FILES("workspace/didChangeWatchedFiles"),
    SYMBOL("workspace/symbol"),
    EXECUTE_COMMAND("workspace/executeCommand"),
    SYNCHRONIZATION("textDocument/synchronization"),
    COMPLETION("textDocument/completion"),
    HOVER("textDocument/hover"),
    SIGNATURE_HELP("textDocument/signatureHelp"),
    REFERENCES("textDocument/references"),
    DOCUMENT_HIGHLIGHT("textDocument/documentHighlight"),
    DOCUMENT_SYMBOL("textDocument/documentSymbol"),
    FORMATTING("textDocument/formatting"),
    RANGE_FORMATTING("textDocument/rangeFormatting"),
    ONTYPE_FORMATTING("textDocument/onTypeFormatting"),
    DEFINITION("textDocument/definition"),
    CODE_ACTION("textDocument/codeAction"),
    CODE_LENS("textDocument/codeLens"),
    DOCUMENT_LINK("textDocument/documentLink"),
    RENAME("textDocument/rename");

    private final String name;

    Methods(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Methods forName(final String name) {
        return Arrays.stream(Methods.values()).filter(n -> n.getName().equals(name)).collect(Collectors.toList()).get(0);
    }

}
