package com.github.gtache.lsp.actions

import com.github.gtache.lsp.editor.services.application.EditorApplicationService
import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.head
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.find.FindBundle
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.ui.LightweightHint
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewUtil
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.UsageViewPresentation
import com.intellij.usages.impl.UsageViewManagerImpl
import java.awt.Color
import javax.swing.JLabel

/**
 * Action for references / see usages (SHIFT+ALT+F7)
 */
class ReferencesAction : DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent): Unit {
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null) {
            val targets = (service<EditorApplicationService>().forEditor(editor)
                ?.references(editor.caretModel.currentCaret.offset, getOriginalElement = true, close = true)?.first
                ?: emptyList())
                .map { r -> PsiElement2UsageTargetAdapter(r, true) }

            showReferences(editor, targets, editor.caretModel.currentCaret.logicalPosition)
        }
    }

    fun forManagerAndOffset(manager: EditorEventManager, offset: Int): Unit {
        val targets = manager.references(offset, getOriginalElement = true, close = true).first.map { r -> PsiElement2UsageTargetAdapter(r, true) }
        val editor = manager.editor
        showReferences(editor, targets, editor.offsetToLogicalPosition(offset))
    }

    fun showReferences(editor: Editor, targets: List<PsiElement2UsageTargetAdapter>, logicalPosition: LogicalPosition): Unit {
        if (targets.isEmpty()) {
            val constraint: Short = HintManager.ABOVE
            val flags: Int = HintManager.HIDE_BY_ANY_KEY or HintManager.HIDE_BY_TEXT_CHANGE or HintManager.HIDE_BY_SCROLLING
            val label = JLabel("No references found")
            label.background = Color(150, 0, 0)
            val hint = LightweightHint(label)
            val p = HintManagerImpl.getHintPosition(hint, editor, logicalPosition, constraint)
            HintManagerImpl.getInstanceImpl()
                .showEditorHint(hint, editor, p, flags, 0, false, HintManagerImpl.createHintHint(editor, p, hint, constraint).setContentActive(false))
        } else {
            val usageInfo = targets.map { ut ->
                val elem = ut.element
                UsageInfo(elem, -1, -1, false)
            }

            editor.project?.let {
                val presentation = createPresentation(targets.head.element, FindUsagesOptions(it), toOpenInNewTab = false)

                UsageViewManagerImpl(it).showUsages(
                    arrayOf(targets.head),
                    usageInfo.map { ui -> UsageInfo2UsageAdapter(ui) }.toTypedArray(),
                    presentation
                )
            }
        }
    }


    private fun createPresentation(psiElement: PsiElement, options: FindUsagesOptions, toOpenInNewTab: Boolean): UsageViewPresentation {
        val presentation = UsageViewPresentation()
        val scopeString = options.searchScope.displayName
        presentation.scopeText = scopeString
        val usagesString = options.generateUsagesString()
        presentation.searchString = usagesString
        val title = FindBundle.message("find.usages.of.element.in.scope.panel.title", usagesString, UsageViewUtil.getLongName(psiElement), scopeString)
        presentation.tabText = title
        presentation.tabName = FindBundle.message("find.usages.of.element.tab.name", usagesString, UsageViewUtil.getShortName(psiElement))
        presentation.targetsNodeText = StringUtil.capitalize(UsageViewUtil.getType(psiElement))
        presentation.isOpenInNewTab = toOpenInNewTab
        return presentation
    }

}