package com.github.gtache.client.languageserver.serverdefinition

trait UserConfigurableServerDefinitionObject {
  def typ: String
  def fromArray(arr: Array[String]): UserConfigurableServerDefinition
}
