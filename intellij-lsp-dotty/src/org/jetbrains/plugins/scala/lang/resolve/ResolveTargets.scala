package org.jetbrains.plugins.scala.lang.resolve

object ResolveTargets extends Enumeration {
  //No case class marker to reject case class in static import when it has companion object
  val METHOD, VAR, VAL, OBJECT, CLASS, PACKAGE, ANNOTATION = Value
}