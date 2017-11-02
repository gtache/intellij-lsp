package org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef

import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition

class QuasiQuotesInjector extends SyntheticMembersInjector {
  override def injectFunctions(source: ScTypeDefinition): Seq[String] = {
    source match {
      // legacy macro emulation - in 2.10 quasiquotes were implemented by a compiler plugin
      // so we need to manually add QQ interpolator stub
      case _ => Seq.empty
    }
  }
}
