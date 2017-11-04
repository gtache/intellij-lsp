package com.github.gtache.settings;

import com.github.gtache.PluginMain;
import com.github.gtache.client.languageserver.serverdefinition.ArtifactLanguageServerDefinition;
import com.github.gtache.client.languageserver.serverdefinition.ExeLanguageServerDefinition;
import com.github.gtache.client.languageserver.serverdefinition.LanguageServerDefinition;
import com.github.gtache.utils.Utils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

/**
 * Class used to manage the settings related to the LSP
 */
public final class LSPSettings implements Configurable {

    private static final Logger LOG = Logger.getInstance(LSPSettings.class);
    @Nullable
    private static LSPGUI lspGUI;
    private static LSPSettings instance;

    private LSPSettings() {
    }

    public static LSPSettings getInstance() {
        if (instance == null) {
            instance = new LSPSettings();
        }
        return instance;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Language Server Protocol";
    }

    @Override
    public String getHelpTopic() {
        return "com.github.gtache.LSPSettings";
    }

    @Override
    public JComponent createComponent() {
        lspGUI = new LSPGUI();
        setGUIFields(PluginMain.getExtToServerDefinitionJava());
        return lspGUI.getRootPanel();
    }

    //TODO complete
    public void setGUIFields(final Map<String, LanguageServerDefinition> map) {
        for (final Map.Entry<String, LanguageServerDefinition> entry : map.entrySet()) {
            final LanguageServerDefinition def = entry.getValue();
            if (def instanceof ArtifactLanguageServerDefinition) {
                final ArtifactLanguageServerDefinition defImpl = (ArtifactLanguageServerDefinition) def;
                lspGUI.getExtField().setText(defImpl.ext());
                lspGUI.getServField().setText(defImpl.packge());
                lspGUI.getMainClassField().setText(defImpl.mainClass());
                lspGUI.getArgsField().setText(Utils.arrayToString(defImpl.args(), " "));
            } else if (def instanceof ExeLanguageServerDefinition) {
                final ExeLanguageServerDefinition defImpl = (ExeLanguageServerDefinition) def;
                lspGUI.getExtField().setText(defImpl.ext());
                lspGUI.getServField().setText(defImpl.path());
                lspGUI.getArgsField().setText(Utils.arrayToString(defImpl.args(), " "));
            }
        }
    }

    @Override
    public boolean isModified() {
        return lspGUI.isModified();
    }

    @Override
    public void apply() {
        lspGUI.apply();
    }

    @Override
    public void reset() {
        lspGUI.reset();
    }

    @Override
    public void disposeUIResources() {
        lspGUI = null;
    }
}
