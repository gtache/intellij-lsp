package org.jetbrains.plugins.dotty.lang.parser

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.tree.IStubFileElementType
import org.jetbrains.plugins.scala.lang.parser.{PsiCreator, ScalaElementTypes, ScalaParserDefinition}

/**
  * @author adkozlov
  */
class DottyParserDefinition extends ScalaParserDefinition {
  override protected val psiCreator: PsiCreator = DottyPsiCreator

  override def createParser(project: Project): DottyParser = new DottyParser

}
