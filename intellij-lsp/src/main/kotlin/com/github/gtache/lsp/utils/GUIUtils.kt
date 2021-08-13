package com.github.gtache.lsp.utils

import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.github.gtache.lsp.contributors.icon.LSPDefaultIconProvider
import com.github.gtache.lsp.contributors.icon.LSPIconProvider
import com.github.gtache.lsp.head
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.ui.Hint
import com.intellij.ui.LightweightHint
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JLabel

/**
 * Various utility methods related to the interface
 */
object GUIUtils {
    private val logger: Logger = Logger.getInstance(GUIUtils::class.java)

    /**
     * Shows a hint in the editor
     *
     * @param editor     The editor
     * @param string     The message / text of the hint
     * @param point      The position of the hint
     * @param constraint The constraint (under/above)
     * @param flags      The flags (when the hint will disappear)
     * @return The hint
     */
    fun createAndShowEditorHint(
        editor: Editor, string: String, point: Point, constraint: Short = HintManager.ABOVE,
        flags: Int = HintManager.HIDE_BY_ANY_KEY or HintManager.HIDE_BY_TEXT_CHANGE or HintManager.HIDE_BY_SCROLLING
    ): Hint? {
        val (hint, p) = createHint(editor, string, point, constraint)
        return if (p != null) {
            HintManagerImpl.getInstanceImpl().showEditorHint(
                hint,
                editor,
                p,
                flags,
                0,
                false,
                HintManagerImpl.createHintHint(editor, p, hint, constraint).setContentActive(false)
            )
            hint
        } else {
            null
        }
    }

    private fun createHint(editor: Editor, string: String, point: Point, constraint: Short): Pair<LightweightHint, Point?> {
        val hint = LightweightHint(JLabel(string))
        val p = if (!editor.isDisposed) HintManagerImpl.getHintPosition(hint, editor, editor.xyToLogicalPosition(point), constraint) else null
        return Pair(hint, p)
    }

    fun createAndShowHint(
        component: JComponent, string: String, point: RelativePoint,
        flags: Int = HintManager.HIDE_BY_ANY_KEY or HintManager.HIDE_BY_TEXT_CHANGE or HintManager.HIDE_BY_OTHER_HINT or HintManager.HIDE_BY_SCROLLING
    ): Unit {
        return HintManagerImpl.getInstanceImpl().showHint(component, point, flags, 0)
    }

    /**
     * Returns a suitable LSPIconProvider given a ServerDefinition
     *
     * @param serverDefinition The serverDefinition
     * @return The LSPIconProvider, or LSPDefaultIconProvider if none are found
     */
    fun getIconProviderFor(serverDefinition: LanguageServerDefinition): LSPIconProvider {
        return try {
            val providers = LSPIconProvider.EP_NAME.extensions.filter { provider -> provider.isSpecificFor(serverDefinition) }
            if (providers.isNotEmpty()) providers.head else LSPDefaultIconProvider
        } catch (e: IllegalArgumentException) {
            LSPDefaultIconProvider
        }
    }
}