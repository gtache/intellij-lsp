package com.github.gtache.settings;

import com.github.gtache.PluginMain;
import com.github.gtache.utils.Utils;
import com.github.gtache.client.languageserver.ArtifactLanguageServerDefinition;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@State(name = "LSPState", storages = @Storage(id = "LSPState", file = "LSPState.xml"))

/**
 * Class representing the state of the LSP settings
 */
public class LSPState implements PersistentStateComponent<LSPState> {

    private static final Logger LOG = Logger.getInstance(LSPState.class);


    public Map<String, String[]> extToServ = new HashMap<>(); //Must be public to be saved

    public LSPState() {
    }

    @Nullable
    public static LSPState getInstance() {
        return ServiceManager.getService(LSPState.class);
    }

    public String getFirstExt() {
        final Map.Entry<String, String[]> entry = extToServ.isEmpty() ? null : extToServ.entrySet().iterator().next();
        return entry == null ? "" : entry.getKey();
    }

    public ArtifactLanguageServerDefinition getFirstServerDefinition() {
        final Map.Entry<String, String[]> entry = extToServ.isEmpty() ? null : extToServ.entrySet().iterator().next();
        return entry == null ? null : Utils.arrayToServerDefinitionArtifact(entry.getValue());
    }

    public Map<String, ArtifactLanguageServerDefinition> getExtToServ() {
        return Utils.arrayMapToServerDefinitionArtifactMap(extToServ);
    }

    public void setExtToServ(final Map<String, ArtifactLanguageServerDefinition> extToServ) {
        this.extToServ = Utils.serverDefinitionArtifactMapToArrayMap(extToServ);
    }

    @Nullable
    @Override
    public LSPState getState() {
        return this;
    }

    @Override
    public void loadState(final LSPState lspState) {
        LOG.info("LSP State loaded");
        XmlSerializerUtil.copyBean(lspState, this);
        PluginMain.setExtToServerDefinition(Utils.arrayMapToServerDefinitionArtifactMap(extToServ));
    }

}
