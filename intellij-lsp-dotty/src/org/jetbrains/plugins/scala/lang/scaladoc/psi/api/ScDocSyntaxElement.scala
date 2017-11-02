package org.jetbrains.plugins.scala.lang.scaladoc.psi.api

import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement

/**
 * User: Dmitry Naidanov
 * Date: 11/14/11
 */

trait ScDocSyntaxElement extends ScalaPsiElement {
  private var flags: Int = 0
  
  def getFlags: Int = flags
  
  def setFlag(flag: Int) {
    flags |= flag
  }
  
  def reverseFlag(flag: Int) {
    flags ^= flag
  }
  
  def clearFlag(flag: Int) {
    flags &= ~flag
  }
  
  def clearAll() {
    flags = 0
  }
}