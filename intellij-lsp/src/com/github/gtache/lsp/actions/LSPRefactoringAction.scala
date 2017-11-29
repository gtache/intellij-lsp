package com.github.gtache.lsp.actions

import java.awt.Color
import java.util

import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.settings.LSPState
import com.github.gtache.lsp.utils.ApplicationUtils
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.{EditorColors, EditorColorsManager}
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.{Messages, NonEmptyInputValidator}

/**
  * Action called when the user presses SHIFT+ALT+F6 to rename a symbol
  */
class LSPRefactoringAction extends DumbAwareAction {
  private val LOG: Logger = Logger.getInstance(classOf[LSPRefactoringAction])

  override def actionPerformed(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    val psiFile = e.getData(CommonDataKeys.PSI_FILE)
    /*if (LanguageRefactoringSupport.INSTANCE.allForLanguage(psiFile.getLanguage).isEmpty) {*/
    EditorEventManager.forEditor(editor) match {
      case Some(manager) =>
        if (LSPState.getInstance().isUseInplaceRename) {
          val offset = editor.getCaretModel.getCurrentCaret.getOffset
          ApplicationUtils.pool(() => {
            val references = manager.documentReferences(offset)
            ApplicationUtils.invokeLater(() => {
              val markers = references.map(r => {
                val marker = editor.getDocument.createRangeMarker(r._1, r._2)
                marker.setGreedyToLeft(true)
                marker.setGreedyToRight(true)
                marker
              })
              val colorsManager = EditorColorsManager.getInstance()
              val highlightManager = HighlightManager.getInstance(editor.getProject)
              val highlighters: util.List[RangeHighlighter] = new util.ArrayList[RangeHighlighter]()
              val rangeToAttributes = markers.map(r => {
                if (r.getStartOffset <= offset && r.getEndOffset >= offset) {
                  val attributes = colorsManager.getGlobalScheme.getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES)
                  attributes.setBackgroundColor(Color.RED)
                  (r, attributes)
                } else {
                  val attributes = colorsManager.getGlobalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)
                  (r, attributes)
                }
              })
              rangeToAttributes.foreach {
                case (r, t) => highlightManager.addOccurrenceHighlight(editor, r.getStartOffset, r.getEndOffset, t, HighlightManager.HIDE_BY_ESCAPE, highlighters, null)
              }

              import scala.collection.JavaConverters._
              highlighters.asScala.foreach(highlighter => {
                highlighter.setGreedyToLeft(true)
                highlighter.setGreedyToRight(true)
              })

              val originalMarker = markers.find(r => r.getStartOffset <= offset && r.getEndOffset >= offset)
              originalMarker match {
                case Some(m) => manager.startRename(m, markers, highlighters.asScala)
                case _ =>
                  val renameTo = Messages.showInputDialog(e.getProject, "Enter new name: ", "Rename", Messages.getQuestionIcon, "", new NonEmptyInputValidator())
                  if (renameTo != null && renameTo != "") manager.rename(renameTo)
              }
            })
          })
        } else {
          val renameTo = Messages.showInputDialog(e.getProject, "Enter new name: ", "Rename", Messages.getQuestionIcon, "", new NonEmptyInputValidator())
          if (renameTo != null && renameTo != "") manager.rename(renameTo)
        }

      case None =>
      /*}*/
    } //else pass to default refactoring
  }
}
