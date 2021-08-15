package com.github.gtache.lsp.settings.gui;

import com.github.gtache.lsp.settings.LSPApplicationState;
import com.github.gtache.lsp.utils.Utils;
import com.github.gtache.lsp.utils.aether.AetherImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public final class ArtifactGUI implements LSPGUI {
    private static final Logger logger = Logger.getInstance(ArtifactGUI.class);

    private static final String placeholder = Utils.getBundle().getString("coursier.settings.textarea.basetext");
    private JPanel rootPanel;
    private JTextArea repositoriesTextArea;
    private JLabel repositoriesLabel;
    private final LSPApplicationState state;

    public ArtifactGUI() {
        state = ApplicationManager.getApplication().getService(LSPApplicationState.class);
        final String str = getStateString();
        repositoriesTextArea.setText(str.isEmpty() ? placeholder : str);
    }

    @Override
    public boolean isModified() {
        final String text = repositoriesTextArea.getText();
        return !placeholder.equals(text) && !text.equals(getStateString()) && AetherImpl.checkRepositories(text, false);
    }

    @Override
    public void reset() {
        repositoriesTextArea.setText(getStateString().isEmpty() ? placeholder : getStateString());
    }

    @Override
    public void apply() {
        if (state != null) {
            final String text = repositoriesTextArea.getText();
            if (text.trim().isEmpty() || text.equals(placeholder)) {
                state.setAdditionalRepositories(Collections.emptyList());
            } else if (AetherImpl.checkRepositories(text, true)) {
                state.setAdditionalRepositories(Arrays.stream(text.split(Utils.getLineSeparator())).collect(Collectors.toList()));
            }
        } else {
            logger.warn("Null state");
        }
    }

    @Override
    public @NotNull JPanel getRootPanel() {
        return rootPanel;
    }

    private String getStateString() {
        return state != null ? String.join(Utils.getLineSeparator(), state.getAdditionalRepositories()) : "";
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        repositoriesLabel = new JLabel();
        repositoriesLabel.setText("Additional repositories");
        rootPanel.add(repositoriesLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        rootPanel.add(spacer2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        repositoriesTextArea = new JTextArea();
        repositoriesTextArea.setMaximumSize(new Dimension(420, 2147483647));
        repositoriesTextArea.setMinimumSize(new Dimension(300, 10));
        repositoriesTextArea.setPreferredSize(new Dimension(300, 50));
        repositoriesTextArea.setText(ResourceBundle.getBundle("com/github/gtache/lsp/LSPBundle").getString("coursier.settings.textarea.basetext"));
        repositoriesTextArea.setToolTipText("Insert one resolver by line");
        scrollPane1.setViewportView(repositoriesTextArea);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
