package com.github.gtache.client.languageserver

trait UserConfigurableServerDefinitionObject {
  def typ: String
  def fromArray(arr: Array[String]): UserConfigurableServerDefinition
}
