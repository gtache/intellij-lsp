package com.github.gtache.lsp.settings.project.converters

import com.github.gtache.lsp.languageserver.settings.Settings
import com.github.gtache.lsp.languageserver.settings.SettingsImpl
import com.intellij.util.xml.ConvertContext

class AdvancedDefinitionSettingsConverter : AbstractConverter<Settings>() {

    override fun toString(t: Settings?, context: ConvertContext?): String? {
        return t?.let { wrap(it.isLogging, it.logDir, it.isAlwaysSendRequests) }
    }

    override fun fromString(s: String?, context: ConvertContext?): Settings? {
        return s?.let {
            val list = unwrap(s)
            return SettingsImpl(list[0].toBooleanStrict(), list[1], list[2].toBooleanStrict())
        }
    }
}