package com.github.gtache.settings;

import com.github.gtache.PluginMain;
import com.github.gtache.ServerDefinitionExtensionPoint;
import com.github.gtache.Utils;
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
    public void setGUIFields(final Map<String, ServerDefinitionExtensionPoint> map) {
        for (final Map.Entry<String, ServerDefinitionExtensionPoint> entry : map.entrySet()) {
            final ServerDefinitionExtensionPoint def = entry.getValue();
            if (def != null) {
                lspGUI.getExtField().setText(def.ext());
                lspGUI.getServField().setText(def.packge());
                lspGUI.getMainClassField().setText(def.mainClass());
                lspGUI.getArgsField().setText(Utils.arrayToString(def.args(), " "));
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
