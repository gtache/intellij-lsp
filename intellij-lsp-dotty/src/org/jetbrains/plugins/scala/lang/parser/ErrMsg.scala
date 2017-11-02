package org.jetbrains.plugins.scala.lang.parser

import org.jetbrains.annotations.PropertyKey
import org.jetbrains.plugins.scala.ScalaBundle

/**
* @author ilyas
*/

object ErrMsg{
  def apply(@PropertyKey(resourceBundle = "org.jetbrains.plugins.scala.ScalaBundle") msg: String): String = {
    ScalaBundle.message(msg)
  }
}