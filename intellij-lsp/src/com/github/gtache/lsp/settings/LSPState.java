package com.github.gtache.lsp.settings;

import com.github.gtache.lsp.PluginMain;
import com.github.gtache.lsp.client.languageserver.serverdefinition.UserConfigurableServerDefinition;
import com.github.gtache.lsp.client.languageserver.serverdefinition.UserConfigurableServerDefinition$;
import com.github.gtache.lsp.requests.Timeout;
import com.github.gtache.lsp.requests.Timeouts;
import com.github.gtache.lsp.utils.ApplicationUtils$;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@State(name = "LSPState", storages = @Storage(file = "LSPState.xml"))

/**
 * Class representing the state of the LSP settings
 */
public final class LSPState implements PersistentStateComponent<LSPState> {

    private static final Logger LOG = Logger.getInstance(LSPState.class);

    //Must be public to be saved
    public boolean logServersOutput;
    public boolean alwaysSendRequests;
    public Map<String, String[]> extToServ;
    public Map<Timeouts, Integer> timeouts;
    public List<String> coursierResolvers;
    public Map<String[], String[]> forcedAssociations;

    public LSPState() {
        alwaysSendRequests = false;
        extToServ = new LinkedHashMap<>(10);
        timeouts = new EnumMap<>(Timeouts.class);
        coursierResolvers = new ArrayList<>(5);
        forcedAssociations = new HashMap<>(10);
    }

    @Nullable
    public static LSPState getInstance() {
        try {
            return ServiceManager.getService(LSPState.class);
        } catch (final Exception e) {
            LOG.warn("Couldn't load LSPState : " + e);
            ApplicationUtils$.MODULE$.invokeLater(() -> Messages.showErrorDialog("Couldn't load LSP settings, you will need to reconfigure them.", "LSP plugin"));
            return null;
        }
    }

    public List<String> getCoursierResolvers() {
        return coursierResolvers;
    }

    public void setCoursierResolvers(final Collection<String> coursierResolvers) {
        this.coursierResolvers = new ArrayList<>(coursierResolvers);
    }

    public Map<String, UserConfigurableServerDefinition> getExtToServ() {
        return UserConfigurableServerDefinition$.MODULE$.fromArrayMap(extToServ);
    }

    public void setExtToServ(final Map<String, UserConfigurableServerDefinition> extToServ) {
        this.extToServ = UserConfigurableServerDefinition$.MODULE$.toArrayMap(extToServ);
    }

    public boolean isAlwaysSendRequests() {
        return alwaysSendRequests;
    }

    public void setAlwaysSendRequests(final boolean b) {
        this.alwaysSendRequests = b;
    }

    public boolean isLoggingServersOutput() {
        return logServersOutput;
    }

    public void setLogServersOutput(final boolean b) {
        this.logServersOutput = b;
    }

    public Map<Timeouts, Integer> getTimeouts() {
        return timeouts;
    }

    public void setTimeouts(final Map<Timeouts, Integer> timeouts) {
        this.timeouts = new EnumMap<>(timeouts);
    }

    public Map<String[], String[]> getForcedAssociations() {
        return forcedAssociations;
    }

    public void setForcedAssociations(final Map<String[], String[]> forcedAssociations) {
        this.forcedAssociations = new HashMap<>(forcedAssociations);
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(logServersOutput) +
                17 * Boolean.hashCode(alwaysSendRequests) +
                3 * extToServ.hashCode() +
                7 * timeouts.hashCode() +
                11 * coursierResolvers.hashCode() +
                13 * forcedAssociations.hashCode();
    }

    @Override
    public boolean equals(final Object that) {
        if (that instanceof LSPState) {
            final LSPState thatS = (LSPState) that;
            return logServersOutput == thatS.logServersOutput &&
                    alwaysSendRequests == thatS.alwaysSendRequests &&
                    extToServ.equals(thatS.extToServ) &&
                    timeouts.equals(thatS.timeouts) &&
                    coursierResolvers.equals(thatS.coursierResolvers) &&
                    forcedAssociations.equals(thatS.forcedAssociations);
        }
        return false;
    }


    @Override
    public LSPState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull final LSPState lspState) {
        try {
            XmlSerializerUtil.copyBean(lspState, this);
            LOG.info("LSP State loaded");
            if (extToServ != null) {
                PluginMain.setExtToServerDefinition(UserConfigurableServerDefinition$.MODULE$.fromArrayMap(extToServ));
            }
            if (timeouts != null && !timeouts.isEmpty()) {
                Timeout.setTimeouts(timeouts);
            }
            if (forcedAssociations != null) {
                PluginMain.setForcedAssociations(forcedAssociations);
            }
        } catch (final Exception e) {
            LOG.warn("Couldn't load LSPState : " + e);
            ApplicationUtils$.MODULE$.invokeLater(() -> Messages.showErrorDialog("Couldn't load LSP settings, you will need to reconfigure them.", "LSP plugin"));
        }
    }

}
