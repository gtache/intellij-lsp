package com.github.gtache.lsp.contributors

import javax.swing.JComponent

import com.github.gtache.lsp.PluginMain
import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.{DiagnosticRangeHighlighter, EditorEventManager}
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.codeInspection._
import com.intellij.codeInspection.ui.SingleCheckboxOptionsPanel
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.DiagnosticSeverity

/**
  * The inspection tool for LSP
  */
class LSPInspection extends LocalInspectionTool {
  private var bool: Boolean = false

  override def checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array[ProblemDescriptor] = {
    val virtualFile = file.getVirtualFile
    if (PluginMain.isExtensionSupported(virtualFile.getExtension)) {
      val uri = FileUtils.VFSToURIString(virtualFile)

      def descriptorsForManager(m: EditorEventManager): Array[ProblemDescriptor] = {
        val diagnostics = m.getDiagnostics
        diagnostics.collect { case DiagnosticRangeHighlighter(rangeHighlighter, diagnostic) if diagnostic.getSeverity != DiagnosticSeverity.Hint =>
          val start = rangeHighlighter.getStartOffset
          val end = rangeHighlighter.getEndOffset
          if (start < end) {
            val name = m.editor.getDocument.getText(new TextRange(start, end))
            val severity = (diagnostic.getSeverity: @unchecked) match {
              case DiagnosticSeverity.Error => ProblemHighlightType.ERROR
              case DiagnosticSeverity.Warning => ProblemHighlightType.GENERIC_ERROR_OR_WARNING
              case DiagnosticSeverity.Information => ProblemHighlightType.INFORMATION
            }
            val element = LSPPsiElement(name, m.editor.getProject, start, end, file)
            val commands = m.codeAction(element)
            manager.createProblemDescriptor(element, null.asInstanceOf[TextRange], diagnostic.getMessage, severity, isOnTheFly,
              if (commands != null) new LSPQuickFix(uri, commands) else null)
          } else null
        }.toArray.filter(d => d != null)
      }

      EditorEventManager.forUri(uri) match {
        case Some(m) =>
          descriptorsForManager(m)
        case None =>
          if (isOnTheFly) {
            super.checkFile(file, manager, isOnTheFly)
          } else {
            /*val descriptor = new OpenFileDescriptor(manager.getProject, virtualFile)
            ApplicationUtils.writeAction(() => FileEditorManager.getInstance(manager.getProject).openTextEditor(descriptor, false))
            EditorEventManager.forUri(uri) match {
              case Some(m) => descriptorsForManager(m)
              case None => super.checkFile(file, manager, isOnTheFly)
            }*/
            //TODO need dispatch thread
            super.checkFile(file, manager, isOnTheFly)
          }
      }
    } else super.checkFile(file, manager, isOnTheFly)
  }

  override def getDisplayName: String = getShortName

  override def getShortName: String = "LSP"

  override def createOptionsPanel(): JComponent = {
    new SingleCheckboxOptionsPanel(getShortName, this, "bool")
  }

  override def getID: String = "LSP"

  override def getGroupDisplayName: String = "LSP"

  override def getStaticDescription: String = "Reports errors by the LSP server"

  override def isEnabledByDefault: Boolean = true

}
