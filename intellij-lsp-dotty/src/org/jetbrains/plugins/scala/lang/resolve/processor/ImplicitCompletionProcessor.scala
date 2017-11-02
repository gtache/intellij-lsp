package org.jetbrains.plugins.scala.lang.resolve.processor

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.resolve.ResolveTargets

import scala.collection.Set

class ImplicitCompletionProcessor(override val kinds: Set[ResolveTargets.Value],
                                  override val getPlace: PsiElement)
  extends CompletionProcessor(kinds, getPlace)
