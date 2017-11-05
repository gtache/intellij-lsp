package com.github.gtache.client.languageserver.serverdefinition

/**
  * Trait for companion objects of the UserConfigurableServerDefinition classes
  */
trait UserConfigurableServerDefinitionObject {
  /**
    * @return the type of the server definition
    */
  def typ: String

  /**
    * Transforms an array of string into the corresponding UserConfigurableServerDefinition
    *
    * @param arr The array
    * @return The server definition
    */
  def fromArray(arr: Array[String]): UserConfigurableServerDefinition
}
