package com.github.gtache.lsp.settings.server.parser;

import org.jetbrains.annotations.Nullable;

public enum ConfigType {
    FLAT, JSON, XML;

    @Nullable
    public static ConfigType forExt(final String ext) {
        if ("FLAT".equalsIgnoreCase(ext)) {
            return FLAT;
        } else if ("JSON".equalsIgnoreCase(ext)) {
            return JSON;
        } else if ("XML".equalsIgnoreCase(ext)) {
            return XML;
        } else return null;
    }

    public static String toExt(final ConfigType typ) {
        if (typ == FLAT) {
            return "txt";
        } else if (typ == JSON) {
            return "json";
        } else if (typ == XML) {
            return "xml";
        } else return null;
    }
}
